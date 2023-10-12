package io.datareplication.internal.http;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Value
public class HeaderFieldValue {
    private static final char QUOTE = '"';
    private static final char DELIMITER = ';';
    private static final char ASSIGNMENT_OPERATOR = '=';
    private static final char ESCAPE_SPECIAL_CHAR = '\\';
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
            expect(DELIMITER);
            eatWhitespace();
            String name = parameterName();
            expect(ASSIGNMENT_OPERATOR);
            eatWhitespace();
            String value = parameterValue();
            parameters.put(name, value);
        }

        private String parameterName() {
            final int start = idx;
            advanceWhile(c -> c != ASSIGNMENT_OPERATOR && c != DELIMITER);
            return input.substring(start, idx).trim().toLowerCase(Locale.ENGLISH);
        }

        private String parameterValue() {
            if (atEnd()) {
                return "";
            }
            if (peek() == QUOTE) {
                return quotedString();
            }
            final int start = idx;
            advanceWhile(c -> c != DELIMITER);
            return input.substring(start, idx).trim();
        }

        private String quotedString() {
            expect(QUOTE);
            final StringBuilder value = new StringBuilder();
            while (!atEnd() && peek() != QUOTE) {
                if (peek() == ESCAPE_SPECIAL_CHAR) {
                    idx++;
                }
                value.append(peek());
                idx++;
            }
            expect(QUOTE);
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
        int mainValueEnd = headerField.indexOf(DELIMITER);
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
