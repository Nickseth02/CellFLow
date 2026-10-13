
package worklet1.parser;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

    private String input;
    private int position;

    public List<Token> tokenize(String formula) {
        if (formula == null) {
            throw new FormulaParseException("Formula cannot be null");
        }

        input = formula.trim();
        position = 0;

        // The initial '=' introduces a formula; it is not an operator.
        if (input.startsWith("=")) {
            position++;
        }

        List<Token> tokens = new ArrayList<>();

        while (position < input.length()) {
            char ch = input.charAt(position);

            if (Character.isWhitespace(ch)) {
                position++;
                continue;
            }

            int start = position;

            if (Character.isDigit(ch)
                    || (ch == '.' && hasNextDigit())) {
                readNumber(tokens);
                continue;
            }

            if (Character.isLetter(ch) || ch == '_') {
                readIdentifier(tokens);
                continue;
            }

            if (ch == '"') {
                readString(tokens);
                continue;
            }

            switch (ch) {
                case '+':
                    add(tokens, TokenType.PLUS, start, 1);
                    break;
                case '-':
                    add(tokens, TokenType.MINUS, start, 1);
                    break;
                case '*':
                    add(tokens, TokenType.MULTIPLY, start, 1);
                    break;
                case '/':
                    add(tokens, TokenType.DIVIDE, start, 1);
                    break;
                case '^':
                    add(tokens, TokenType.POWER, start, 1);
                    break;
                case '&':
                    add(tokens, TokenType.CONCAT, start, 1);
                    break;
                case '(':
                    add(tokens, TokenType.LEFT_PAREN, start, 1);
                    break;
                case ')':
                    add(tokens, TokenType.RIGHT_PAREN, start, 1);
                    break;
                case ',':
                    add(tokens, TokenType.COMMA, start, 1);
                    break;
                case ':':
                    add(tokens, TokenType.COLON, start, 1);
                    break;
                case '=':
                    add(tokens, TokenType.EQUAL, start, 1);
                    break;
                case '<':
                    readLessOperator(tokens);
                    break;
                case '>':
                    readGreaterOperator(tokens);
                    break;
                default:
                    throw error("Unexpected character '" + ch + "'", start);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }

    private boolean hasNextDigit() {
        return position + 1 < input.length()
                && Character.isDigit(input.charAt(position + 1));
    }

    private void readNumber(List<Token> tokens) {
        int start = position;
        boolean decimalSeen = false;

        while (position < input.length()) {
            char ch = input.charAt(position);

            if (Character.isDigit(ch)) {
                position++;
            } else if (ch == '.' && !decimalSeen) {
                decimalSeen = true;
                position++;
            } else {
                break;
            }
        }

        // Optional scientific notation, e.g. 1.5E3.
        if (position < input.length()
                && (input.charAt(position) == 'e'
                || input.charAt(position) == 'E')) {

            int exponentStart = position;
            position++;

            if (position < input.length()
                    && (input.charAt(position) == '+'
                    || input.charAt(position) == '-')) {
                position++;
            }

            int digitStart = position;

            while (position < input.length()
                    && Character.isDigit(input.charAt(position))) {
                position++;
            }

            if (digitStart == position) {
                throw error("Invalid number exponent", exponentStart);
            }
        }

        String text = input.substring(start, position);

        try {
            Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw error("Invalid number '" + text + "'", start);
        }

        tokens.add(new Token(TokenType.NUMBER, text, start));
    }

    private void readIdentifier(List<Token> tokens) {
        int start = position;

        while (position < input.length()) {
            char ch = input.charAt(position);

            if (Character.isLetterOrDigit(ch) || ch == '_') {
                position++;
            } else {
                break;
            }
        }

        String text = input.substring(start, position);
        tokens.add(new Token(TokenType.IDENTIFIER, text, start));
    }

    private void readString(List<Token> tokens) {
        int start = position;
        position++; // Skip opening quote.

        StringBuilder value = new StringBuilder();

        while (position < input.length()) {
            char ch = input.charAt(position++);

            if (ch == '"') {
                // Two consecutive quotes represent a quote in the text.
                if (position < input.length()
                        && input.charAt(position) == '"') {
                    value.append('"');
                    position++;
                } else {
                    tokens.add(new Token(
                            TokenType.STRING, value.toString(), start));
                    return;
                }
            } else {
                value.append(ch);
            }
        }

        throw error("Unterminated string", start);
    }

    private void readLessOperator(List<Token> tokens) {
        int start = position;

        if (position + 1 < input.length()) {
            char next = input.charAt(position + 1);

            if (next == '=') {
                add(tokens, TokenType.LESS_EQUAL, start, 2);
                return;
            }

            if (next == '>') {
                add(tokens, TokenType.NOT_EQUAL, start, 2);
                return;
            }
        }

        add(tokens, TokenType.LESS_THAN, start, 1);
    }

    private void readGreaterOperator(List<Token> tokens) {
        int start = position;

        if (position + 1 < input.length()
                && input.charAt(position + 1) == '=') {
            add(tokens, TokenType.GREATER_EQUAL, start, 2);
            return;
        }

        add(tokens, TokenType.GREATER_THAN, start, 1);
    }

    private void add(List<Token> tokens, TokenType type,
                     int start, int length) {
        String text = input.substring(start, start + length);
        tokens.add(new Token(type, text, start));
        position += length;
    }

    private FormulaParseException error(String message, int at) {
        return new FormulaParseException(
                message + " at position " + at);
    }
}
