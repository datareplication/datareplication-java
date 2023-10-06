package io.datareplication.internal.http;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Value
public class HeaderFieldValue {
    String mainValue;
    @Getter(AccessLevel.PRIVATE)
    Map<String, String> parameters;

    public Optional<String> parameter(String name) {
        return Optional.ofNullable(parameters.get(name));
    }

    private static class ParameterParser {
        private final String input;
        private int idx;
        private final Map<String, String> parameters;

        private ParameterParser(final String input, final int idx) {
            this.input = input;
            this.idx = idx;
            this.parameters = new HashMap<>();
        }

        private void parseNextParameter() {
            expect(';');
            eatWhitespace();
            String name = parameterName();
            expect('=');
            eatWhitespace();
            String value = parameterValue();
            parameters.put(name, value);
        }

        private String parameterName() {
            final int start = idx;
            advanceWhile(c -> c != '=' && c != ';');
            return input.substring(start, idx).trim().toLowerCase();
        }

        private String parameterValue() {
            if (atEnd()) {
                return "";
            }
            if (peek() == '"') {
                return quotedString();
            }
            final int start = idx;
            advanceWhile(c -> c != ';');
            return input.substring(start, idx).trim();
        }

        private String quotedString() {
            expect('"');
            final StringBuilder value = new StringBuilder();
            while (!atEnd() && peek() != '"') {
                if (peek() == '\\') {
                    idx++;
                }
                value.append(peek());
                idx++;
            }
            expect('"');
            eatWhitespace();
            return value.toString();
        }

        private void advanceWhile(Predicate<Character> condition) {
            while (!atEnd() && condition.test(peek())) {
                idx++;
            }
        }

        private void eatWhitespace() {
            advanceWhile(Character::isWhitespace);
        }

        private void expect(char expected) {
            if (atEnd()) {
                throw new IllegalArgumentException(String.format("expected %s, found EOF", expected));
            }
            final char actual = peek();
            if (actual != expected) {
                throw new IllegalArgumentException(String.format("expected %s, found %s", expected, actual));
            }
            idx++;
        }

        private char peek() {
            if (atEnd()) {
                throw new IllegalArgumentException("expected more input, found EOF");
            }
            return input.charAt(idx);
        }

        private boolean atEnd() {
            return idx >= input.length();
        }
    }

    public static HeaderFieldValue parse(String headerField) {
        int mainValueEnd = headerField.indexOf(";");
        if (mainValueEnd == -1) {
            return new HeaderFieldValue(headerField.trim(), Collections.emptyMap());
        }
        final String mainValue = headerField.substring(0, mainValueEnd).trim();
        final ParameterParser parser = new ParameterParser(headerField, mainValueEnd);
        while (!parser.atEnd()) {
            parser.parseNextParameter();
        }
        return new HeaderFieldValue(mainValue, parser.parameters);
    }
}
