package com.nzr.corretor;

import android.content.Context;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SpellCorrectionEngine implements SpellCheckerSession.SpellCheckerSessionListener {
    public interface Callback {
        void onSuccess(String correctedText, int replacements);
        void onUnavailable(String reason);
    }

    private static final int MAX_CHUNK = 900;

    private final Context context;
    private SpellCheckerSession session;
    private Callback callback;
    private List<String> chunks;
    private final StringBuilder result = new StringBuilder();
    private int chunkIndex = 0;
    private int replacements = 0;
    private boolean finished = false;

    public SpellCorrectionEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public void correct(String text, Callback callback) {
        this.callback = callback;
        this.chunks = splitText(text == null ? "" : text);
        this.result.setLength(0);
        this.chunkIndex = 0;
        this.replacements = 0;
        this.finished = false;

        TextServicesManager tsm = (TextServicesManager) context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        if (tsm == null) {
            callback.onUnavailable("O serviço de correção do Android não está disponível neste aparelho.");
            return;
        }

        Locale ptBr = new Locale("pt", "BR");
        session = tsm.newSpellCheckerSession(null, ptBr, this, true);
        if (session == null) {
            callback.onUnavailable("Ative o corretor ortográfico do Android em Configurações > Sistema > Idiomas e entrada > Corretor ortográfico.");
            return;
        }

        if (chunks.isEmpty()) {
            finish();
            return;
        }
        requestCurrentChunk();
    }

    private void requestCurrentChunk() {
        if (finished) return;
        if (chunkIndex >= chunks.size()) {
            finish();
            return;
        }
        String chunk = chunks.get(chunkIndex);
        if (chunk.trim().isEmpty()) {
            result.append(chunk);
            chunkIndex++;
            requestCurrentChunk();
            return;
        }
        session.getSentenceSuggestions(new TextInfo[]{new TextInfo(chunk)}, 5);
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        if (finished || chunkIndex >= chunks.size()) return;
        String original = chunks.get(chunkIndex);
        String corrected = applySuggestions(original, results);
        result.append(corrected);
        chunkIndex++;
        requestCurrentChunk();
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
    }

    private String applySuggestions(String text, SentenceSuggestionsInfo[] sentenceResults) {
        if (sentenceResults == null || sentenceResults.length == 0 || sentenceResults[0] == null) {
            return text;
        }

        SentenceSuggestionsInfo sentence = sentenceResults[0];
        List<Replacement> edits = new ArrayList<>();

        for (int i = 0; i < sentence.getSuggestionsCount(); i++) {
            SuggestionsInfo info = sentence.getSuggestionsInfoAt(i);
            if (info == null) continue;

            int attrs = info.getSuggestionsAttributes();
            boolean typo = (attrs & SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0;
            if (!typo || info.getSuggestionsCount() <= 0) continue;

            int start = sentence.getOffsetAt(i);
            int length = sentence.getLengthAt(i);
            if (start < 0 || length <= 0 || start + length > text.length()) continue;

            String current = text.substring(start, start + length);
            String suggestion = info.getSuggestionAt(0);
            if (suggestion == null || suggestion.trim().isEmpty()) continue;
            suggestion = matchCase(current, suggestion);
            if (current.equals(suggestion)) continue;

            edits.add(new Replacement(start, start + length, suggestion));
        }

        Collections.sort(edits, new Comparator<Replacement>() {
            @Override
            public int compare(Replacement a, Replacement b) {
                return Integer.compare(b.start, a.start);
            }
        });

        StringBuilder sb = new StringBuilder(text);
        int lastStart = text.length() + 1;
        for (Replacement edit : edits) {
            if (edit.end > lastStart) continue;
            sb.replace(edit.start, edit.end, edit.value);
            lastStart = edit.start;
            replacements++;
        }
        return sb.toString();
    }

    private static String matchCase(String original, String suggestion) {
        if (original.length() == 0) return suggestion;
        if (original.equals(original.toUpperCase(new Locale("pt", "BR")))) {
            return suggestion.toUpperCase(new Locale("pt", "BR"));
        }
        if (Character.isUpperCase(original.charAt(0))) {
            if (suggestion.length() == 1) return suggestion.toUpperCase(new Locale("pt", "BR"));
            return suggestion.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + suggestion.substring(1);
        }
        return suggestion;
    }

    private static List<String> splitText(String text) {
        List<String> out = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + MAX_CHUNK);
            if (end < text.length()) {
                int best = -1;
                for (int i = end; i > start + MAX_CHUNK / 2; i--) {
                    char c = text.charAt(i - 1);
                    if (c == '\n' || c == '.' || c == '!' || c == '?' || c == ';' || c == ' ') {
                        best = i;
                        break;
                    }
                }
                if (best > start) end = best;
            }
            out.add(text.substring(start, end));
            start = end;
        }
        if (text.isEmpty()) out.add("");
        return out;
    }

    private void finish() {
        if (finished) return;
        finished = true;
        if (session != null) {
            session.close();
            session = null;
        }
        callback.onSuccess(result.toString(), replacements);
    }

    public void cancel() {
        finished = true;
        if (session != null) {
            session.close();
            session = null;
        }
    }

    private static final class Replacement {
        final int start;
        final int end;
        final String value;

        Replacement(int start, int end, String value) {
            this.start = start;
            this.end = end;
            this.value = value;
        }
    }
}
