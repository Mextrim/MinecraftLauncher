package com.craftlauncher;

import java.util.*;

public class MiniJson {

    public enum Type { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    public static class Value {
        public final Type type;
        public final Object raw;

        public Value(Type type, Object raw) {
            this.type = type;
            this.raw = raw;
        }

        public boolean isObject() { return type == Type.OBJECT; }
        public boolean isArray() { return type == Type.ARRAY; }
        public boolean isString() { return type == Type.STRING; }
        public boolean isNumber() { return type == Type.NUMBER; }
        public boolean isBoolean() { return type == Type.BOOLEAN; }
        public boolean isNull() { return type == Type.NULL || raw == null; }

        @SuppressWarnings("unchecked")
        public Map<String, Value> asObject() {
            if (raw instanceof Map) return (Map<String, Value>) raw;
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        public List<Value> asArray() {
            if (raw instanceof List) return (List<Value>) raw;
            return Collections.emptyList();
        }

        public String asString() { return asString(""); }
        public String asString(String def) {
            if (raw != null) return raw.toString();
            return def;
        }

        public int asInt(int def) {
            if (raw instanceof Number) return ((Number) raw).intValue();
            try { return Integer.parseInt(asString("")); } catch (Exception e) { return def; }
        }

        public long asLong(long def) {
            if (raw instanceof Number) return ((Number) raw).longValue();
            try { return Long.parseLong(asString("")); } catch (Exception e) { return def; }
        }

        public boolean asBoolean(boolean def) {
            if (raw instanceof Boolean) return (Boolean) raw;
            return def;
        }

        public Value get(String key) {
            if (isObject()) {
                Value v = asObject().get(key);
                if (v != null) return v;
            }
            return new Value(Type.NULL, null);
        }

        public Value get(int index) {
            if (isArray()) {
                List<Value> list = asArray();
                if (index >= 0 && index < list.size()) return list.get(index);
            }
            return new Value(Type.NULL, null);
        }

        @Override
        public String toString() {
            return raw == null ? "null" : raw.toString();
        }
    }

    public static Value parse(String src) {
        if (src == null) return new Value(Type.NULL, null);
        Parser parser = new Parser(src.trim());
        try {
            return parser.parseValue();
        } catch (Exception e) {
            return new Value(Type.NULL, null);
        }
    }

    private static class Parser {
        private final String s;
        private int pos = 0;

        Parser(String s) { this.s = s; }

        private void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            skipWhitespace();
            if (pos >= s.length()) return '\0';
            return s.charAt(pos);
        }

        private char next() {
            skipWhitespace();
            if (pos >= s.length()) return '\0';
            return s.charAt(pos++);
        }

        Value parseValue() {
            skipWhitespace();
            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            return new Value(Type.NULL, null);
        }

        Value parseObject() {
            next(); // '{'
            Map<String, Value> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                next();
                return new Value(Type.OBJECT, map);
            }
            while (pos < s.length()) {
                Value keyVal = parseString();
                String key = keyVal.asString();
                skipWhitespace();
                if (next() != ':') break;
                Value val = parseValue();
                map.put(key, val);
                skipWhitespace();
                char c = next();
                if (c == '}') break;
                if (c != ',') break;
            }
            return new Value(Type.OBJECT, map);
        }

        Value parseArray() {
            next(); // '['
            List<Value> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                next();
                return new Value(Type.ARRAY, list);
            }
            while (pos < s.length()) {
                Value val = parseValue();
                list.add(val);
                skipWhitespace();
                char c = next();
                if (c == ']') break;
                if (c != ',') break;
            }
            return new Value(Type.ARRAY, list);
        }

        Value parseString() {
            next(); // '"'
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < s.length()) {
                    char esc = s.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= s.length()) {
                                String hex = s.substring(pos, pos + 4);
                                try {
                                    sb.append((char) Integer.parseInt(hex, 16));
                                } catch (Exception ignored) {}
                                pos += 4;
                            }
                            break;
                        default: sb.append(esc); break;
                    }
                } else {
                    sb.append(c);
                }
            }
            return new Value(Type.STRING, sb.toString());
        }

        Value parseNumber() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.' || s.charAt(pos) == 'e' || s.charAt(pos) == 'E' || s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                pos++;
            }
            String numStr = s.substring(start, pos);
            try {
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    return new Value(Type.NUMBER, Double.parseDouble(numStr));
                } else {
                    return new Value(Type.NUMBER, Long.parseLong(numStr));
                }
            } catch (Exception e) {
                return new Value(Type.NUMBER, 0);
            }
        }

        Value parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return new Value(Type.BOOLEAN, true);
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return new Value(Type.BOOLEAN, false);
            }
            return new Value(Type.BOOLEAN, false);
        }

        Value parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
            }
            return new Value(Type.NULL, null);
        }
    }
}
