package groupDGuessWho;

import java.util.*;
import java.util.regex.*;

/** Strict flat JSON decoder for the small control protocol; no external JARs.
 * Requests accept string/number values only. Arrays, nested objects, booleans,
 * nulls, duplicate keys, trailing data and non-JSON number syntax are rejected.
 */
final class ControlJson {
    private final String text;
    private int pos;
    private static final Pattern NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");
    private ControlJson(String text) { this.text = text; }
    private void space() { while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++; }
    private boolean take(char c) { space(); if (pos < text.length() && text.charAt(pos) == c) { pos++; return true; } return false; }
    private void expect(char c) { if (!take(c)) throw new IllegalArgumentException("Expected '" + c + "'"); }
    private String string() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') return out.toString();
            if (c < 32) throw new IllegalArgumentException("Unescaped control character");
            if (c == '\\') {
                if (pos == text.length()) throw new IllegalArgumentException("Incomplete escape");
                c = text.charAt(pos++);
                switch (c) {
                    case '"': case '\\': case '/': out.append(c); break;
                    case 'b': out.append('\b'); break;
                    case 'f': out.append('\f'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    case 'u':
                        if (pos + 4 > text.length() || !text.substring(pos, pos+4).matches("[0-9a-fA-F]{4}"))
                            throw new IllegalArgumentException("Invalid Unicode escape");
                        out.append((char)Integer.parseInt(text.substring(pos, pos+4),16)); pos += 4; break;
                    default: throw new IllegalArgumentException("Invalid string escape");
                }
            } else out.append(c);
        }
        throw new IllegalArgumentException("Unterminated string");
    }
    static Map<String,Object> parse(String text) {
        ControlJson p = new ControlJson(text);
        Map<String,Object> result = new LinkedHashMap<>();
        p.expect('{');
        if (!p.take('}')) {
            do {
                String key = p.string(); p.expect(':'); p.space();
                Object value;
                if (p.pos < text.length() && text.charAt(p.pos) == '"') value = p.string();
                else {
                    Matcher m = NUMBER.matcher(text); m.region(p.pos,text.length());
                    if (!m.lookingAt()) throw new IllegalArgumentException("Expected string or number");
                    double number = Double.parseDouble(m.group());
                    if (!Double.isFinite(number)) throw new IllegalArgumentException("Number must be finite");
                    value = number; p.pos = m.end();
                }
                if (result.putIfAbsent(key,value) != null) throw new IllegalArgumentException("Duplicate key");
            } while (p.take(','));
            p.expect('}');
        }
        p.space();
        if (p.pos != text.length()) throw new IllegalArgumentException("Trailing JSON data");
        return result;
    }
    static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?,?> map) {
            StringJoiner join = new StringJoiner(",","{","}");
            map.forEach((k,v) -> join.add(encode(k.toString())+":"+encode(v)));
            return join.toString();
        }
        if (value instanceof Iterable<?> list) {
            StringJoiner join = new StringJoiner(",","[","]");
            list.forEach(v -> join.add(encode(v))); return join.toString();
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toString().toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 32 || java.lang.Character.isSurrogate(c)) out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
