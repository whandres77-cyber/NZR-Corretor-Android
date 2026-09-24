package com.nzr.corretor;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalCorrectionEngine {
    private LocalCorrectionEngine() {}

    public static Result correct(String input) {
        if (input == null) input = "";
        String original = input;
        String text = input;

        text = normalizeSpacesAndPunctuation(text);
        text = replaceWords(text);
        text = addBasicCommas(text);
        text = capitalizeSentences(text);
        text = finishSentence(text);

        int changes = text.equals(original) ? 0 : 1;
        return new Result(text, changes);
    }

    private static String normalizeSpacesAndPunctuation(String text) {
        text = text.replaceAll("[ \\t]+([,.;!?])", "$1");
        text = text.replaceAll("([,;!?])(?=[\\p{L}\\p{N}])", "$1 ");
        text = text.replaceAll("[ \\t]{2,}", " ");
        text = text.replaceAll("\\.{3,}", "…");
        return text;
    }

    private static String replaceWords(String text) {
        Map<String, String> words = new LinkedHashMap<>();

        words.put("vc", "você");
        words.put("vcs", "vocês");
        words.put("voce", "você");
        words.put("voces", "vocês");
        words.put("nao", "não");
        words.put("n", "não");
        words.put("tb", "também");
        words.put("tbm", "também");
        words.put("tambem", "também");
        words.put("dps", "depois");
        words.put("agr", "agora");
        words.put("hj", "hoje");
        words.put("ontem", "ontem");
        words.put("amanha", "amanhã");
        words.put("ja", "já");
        words.put("ate", "até");
        words.put("alem", "além");
        words.put("ninguem", "ninguém");
        words.put("alguem", "alguém");
        words.put("oq", "o que");
        words.put("pq", "porque");
        words.put("q", "que");
        words.put("msg", "mensagem");
        words.put("blz", "beleza");
        words.put("td", "tudo");
        words.put("eh", "é");
        words.put("ta", "tá");
        words.put("to", "tô");
        words.put("so", "só");
        words.put("possivel", "possível");
        words.put("impossivel", "impossível");
        words.put("facil", "fácil");
        words.put("dificil", "difícil");
        words.put("otimo", "ótimo");
        words.put("otima", "ótima");
        words.put("pessimo", "péssimo");
        words.put("pessima", "péssima");
        words.put("rapido", "rápido");
        words.put("rapida", "rápida");
        words.put("ultimo", "último");
        words.put("ultima", "última");
        words.put("proximo", "próximo");
        words.put("proxima", "próxima");
        words.put("sabado", "sábado");
        words.put("numero", "número");
        words.put("musica", "música");
        words.put("video", "vídeo");
        words.put("videos", "vídeos");
        words.put("pagina", "página");
        words.put("paginas", "páginas");
        words.put("codigo", "código");
        words.put("codigos", "códigos");
        words.put("aplicativo", "aplicativo");
        words.put("android", "Android");
        words.put("whatsapp", "WhatsApp");
        words.put("discord", "Discord");
        words.put("tiktok", "TikTok");

        for (Map.Entry<String, String> entry : words.entrySet()) {
            text = replaceWholeWord(text, entry.getKey(), entry.getValue());
        }
        return text;
    }

    private static String replaceWholeWord(String text, String from, String to) {
        Pattern p = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}_])" + Pattern.quote(from) + "(?![\\p{L}\\p{N}_])");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String found = m.group();
            String replacement = matchCase(found, to);
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String addBasicCommas(String text) {
        text = text.replaceAll("(?iu)(?<![,;:])\\s+(mas|porém|porem|contudo|entretanto)\\s+", ", $1 ");
        text = text.replaceAll("(?iu)^(por exemplo|ou seja|além disso|alem disso|no entanto)\\s+", "$1, ");
        text = text.replaceAll("(?iu)([.!?]\\s+)(por exemplo|ou seja|além disso|alem disso|no entanto)\\s+", "$1$2, ");
        return text;
    }

    private static String capitalizeSentences(String input) {
        StringBuilder out = new StringBuilder(input);
        boolean capitalize = true;

        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);

            if (capitalize && Character.isLetter(c)) {
                out.setCharAt(i, Character.toUpperCase(c));
                capitalize = false;
                continue;
            }

            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                capitalize = true;
            }
        }
        return out.toString();
    }

    private static String finishSentence(String text) {
        int end = text.length() - 1;
        while (end >= 0 && Character.isWhitespace(text.charAt(end))) end--;
        if (end < 0) return text;

        char last = text.charAt(end);
        if (last == '.' || last == '!' || last == '?' || last == '…' || last == ':' || last == ';') {
            return text;
        }

        if (Character.isLetterOrDigit(last)) {
            String before = text.substring(0, end + 1);
            if (!before.matches("(?i).*https?://\\S+$")) {
                return text.substring(0, end + 1) + "." + text.substring(end + 1);
            }
        }
        return text;
    }

    private static String matchCase(String original, String replacement) {
        if (original.isEmpty() || replacement.isEmpty()) return replacement;
        Locale pt = new Locale("pt", "BR");

        if (original.equals(original.toUpperCase(pt)) && original.length() > 1) {
            return replacement.toUpperCase(pt);
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return replacement.substring(0, 1).toUpperCase(pt) + replacement.substring(1);
        }
        return replacement;
    }

    public static final class Result {
        public final String text;
        public final int changes;

        Result(String text, int changes) {
            this.text = text;
            this.changes = changes;
        }
    }
}
