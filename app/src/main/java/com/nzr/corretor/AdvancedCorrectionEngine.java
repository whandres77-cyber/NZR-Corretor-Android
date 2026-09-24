package com.nzr.corretor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AdvancedCorrectionEngine {
    public interface Callback {
        void onSuccess(String correctedText, int replacements, boolean online);
        void onError(String message);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled = false;

    public AdvancedCorrectionEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public void correct(final String text, final Callback callback) {
        final String source = text == null ? "" : text;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (cancelled) return;
                try {
                    Result remote = checkOnline(source);
                    String polished = polish(remote.text);
                    int extra = polished.equals(remote.text) ? 0 : 1;
                    deliverSuccess(callback, polished, remote.replacements + extra, true);
                } catch (Exception onlineError) {
                    try {
                        String fallback = polish(applySafeAccents(source));
                        int changed = fallback.equals(source) ? 0 : 1;
                        deliverSuccess(callback, fallback, changed, false);
                    } catch (Exception e) {
                        deliverError(callback, "Não foi possível corrigir este texto agora.");
                    }
                }
            }
        });
    }

    private Result checkOnline(String text) throws Exception {
        URL url = new URL("https://api.languagetool.org/v2/check");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "NZR-Corretor/2.0 Android");

        String body = "language=pt-BR&text=" + URLEncoder.encode(text, "UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);

        OutputStream os = connection.getOutputStream();
        os.write(bytes);
        os.flush();
        os.close();

        int code = connection.getResponseCode();
        InputStream is = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        String json = readAll(is);
        connection.disconnect();

        if (code < 200 || code >= 300) {
            throw new IllegalStateException("HTTP " + code);
        }

        JSONObject root = new JSONObject(json);
        JSONArray matches = root.optJSONArray("matches");
        if (matches == null) return new Result(text, 0);

        List<Edit> edits = new ArrayList<>();
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.optJSONObject(i);
            if (match == null) continue;

            int offset = match.optInt("offset", -1);
            int length = match.optInt("length", 0);
            JSONArray replacements = match.optJSONArray("replacements");
            if (offset < 0 || length < 0 || replacements == null || replacements.length() == 0) continue;

            JSONObject first = replacements.optJSONObject(0);
            if (first == null) continue;
            String value = first.optString("value", "");
            if (value.isEmpty()) continue;
            if (offset + length > text.length()) continue;

            String original = text.substring(offset, offset + length);
            value = matchCase(original, value);
            if (!original.equals(value)) {
                edits.add(new Edit(offset, offset + length, value));
            }
        }

        Collections.sort(edits, new Comparator<Edit>() {
            @Override
            public int compare(Edit a, Edit b) {
                return Integer.compare(b.start, a.start);
            }
        });

        StringBuilder sb = new StringBuilder(text);
        int lastStart = text.length() + 1;
        int applied = 0;
        for (Edit edit : edits) {
            if (edit.end > lastStart) continue;
            sb.replace(edit.start, edit.end, edit.value);
            lastStart = edit.start;
            applied++;
        }

        return new Result(sb.toString(), applied);
    }

    private static String polish(String input) {
        String text = applySafeAccents(input);

        text = text.replaceAll("[ \\t]+([,.;!?])", "$1");
        text = text.replaceAll("([,;!?])(?=[\\p{L}\\p{N}])", "$1 ");
        text = text.replaceAll("\\.{3,}", "…");
        text = text.replaceAll("[ \\t]{2,}", " ");
        text = capitalizeSentences(text);

        String trimmed = text.trim();
        if (!trimmed.isEmpty()) {
            char last = trimmed.charAt(trimmed.length() - 1);
            boolean needsEnd = Character.isLetterOrDigit(last);
            boolean looksLikeUrl = trimmed.matches("(?i).*https?://\\S+$");
            if (needsEnd && !looksLikeUrl && trimmed.length() >= 4) {
                int end = text.lastIndexOf(trimmed) + trimmed.length();
                text = text.substring(0, end) + "." + text.substring(end);
            }
        }
        return text;
    }

    private static String capitalizeSentences(String input) {
        StringBuilder out = new StringBuilder(input);
        boolean cap = true;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (cap && Character.isLetter(c)) {
                out.setCharAt(i, Character.toUpperCase(c));
                cap = false;
            }
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                cap = true;
            } else if (!Character.isWhitespace(c) && c != '"' && c != '\'' && c != '(' && c != '[' && c != '{') {
                if (!Character.isLetter(c)) {
                    // keep state for emoji/punctuation; next letter can still start the sentence
                }
            }
        }
        return out.toString();
    }

    private static String applySafeAccents(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("nao", "não");
        map.put("voce", "você");
        map.put("voces", "vocês");
        map.put("tambem", "também");
        map.put("ja", "já");
        map.put("ate", "até");
        map.put("alem", "além");
        map.put("ninguem", "ninguém");
        map.put("alguem", "alguém");
        map.put("possivel", "possível");
        map.put("impossivel", "impossível");
        map.put("facil", "fácil");
        map.put("dificil", "difícil");
        map.put("otimo", "ótimo");
        map.put("otima", "ótima");
        map.put("pessimo", "péssimo");
        map.put("pessima", "péssima");
        map.put("rapido", "rápido");
        map.put("rapida", "rápida");
        map.put("ultimo", "último");
        map.put("ultima", "última");
        map.put("proximo", "próximo");
        map.put("proxima", "próxima");
        map.put("sabado", "sábado");
        map.put("domingo", "domingo");

        StringBuilder out = new StringBuilder();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i <= input.length(); i++) {
            char c = i < input.length() ? input.charAt(i) : '\0';
            if (i < input.length() && Character.isLetter(c)) {
                word.append(c);
            } else {
                if (word.length() > 0) {
                    String original = word.toString();
                    String replacement = map.get(original.toLowerCase(new Locale("pt", "BR")));
                    if (replacement == null) {
                        out.append(original);
                    } else {
                        out.append(matchCase(original, replacement));
                    }
                    word.setLength(0);
                }
                if (i < input.length()) out.append(c);
            }
        }
        return out.toString();
    }

    private static String matchCase(String original, String suggestion) {
        if (original.isEmpty()) return suggestion;
        Locale locale = new Locale("pt", "BR");
        if (original.equals(original.toUpperCase(locale))) {
            return suggestion.toUpperCase(locale);
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return suggestion.substring(0, 1).toUpperCase(locale) + suggestion.substring(1);
        }
        return suggestion;
    }

    private static String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private void deliverSuccess(final Callback callback, final String text, final int replacements, final boolean online) {
        if (cancelled) return;
        main.post(new Runnable() {
            @Override
            public void run() {
                if (!cancelled) callback.onSuccess(text, replacements, online);
            }
        });
    }

    private void deliverError(final Callback callback, final String message) {
        if (cancelled) return;
        main.post(new Runnable() {
            @Override
            public void run() {
                if (!cancelled) callback.onError(message);
            }
        });
    }

    public void cancel() {
        cancelled = true;
        executor.shutdownNow();
    }

    private static final class Edit {
        final int start;
        final int end;
        final String value;

        Edit(int start, int end, String value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }

    private static final class Result {
        final String text;
        final int replacements;

        Result(String text, int replacements) {
            this.text = text;
            this.replacements = replacements;
        }
    }
}
