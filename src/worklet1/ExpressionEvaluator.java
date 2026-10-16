
package worklet1;

import api.EvaluationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates arithmetic expressions, cell references,
 * and basic spreadsheet functions.
 */
public class ExpressionEvaluator {

    private final String input;
    private final EvaluationContext context;
    private int position;

    private ExpressionEvaluator(String input, EvaluationContext context) {
        this.input = input.startsWith("=") ? input.substring(1) : input;
        this.context = context;
        this.position = 0;
    }

    public static Object evaluate(String expression, EvaluationContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Formula cannot be empty");
        }

        ExpressionEvaluator evaluator =
                new ExpressionEvaluator(expression.trim(), context);

        double result = evaluator.parseExpression();
        evaluator.skipSpaces();

        if (evaluator.position != evaluator.input.length()) {
            throw new IllegalArgumentException(
                    "Unexpected character: " + evaluator.peek());
        }

        return result;
    }

    // Addition and subtraction
    private double parseExpression() {
        double value = parseTerm();

        while (true) {
            skipSpaces();

            if (match('+')) {
                value += parseTerm();
            } else if (match('-')) {
                value -= parseTerm();
            } else {
                return value;
            }
        }
    }

    // Multiplication and division
    private double parseTerm() {
        double value = parsePower();

        while (true) {
            skipSpaces();

            if (match('*')) {
                value *= parsePower();
            } else if (match('/')) {
                double divisor = parsePower();

                if (divisor == 0) {
                    throw new ArithmeticException("Division by zero");
                }

                value /= divisor;
            } else {
                return value;
            }
        }
    }

    // Exponentiation
    private double parsePower() {
        double value = parseUnary();
        skipSpaces();

        if (match('^')) {
            value = Math.pow(value, parsePower());
        }

        return value;
    }

    // Unary plus and minus
    private double parseUnary() {
        skipSpaces();

        if (match('+')) {
            return parseUnary();
        }

        if (match('-')) {
            return -parseUnary();
        }

        return parsePrimary();
    }

    private double parsePrimary() {
        skipSpaces();

        if (match('(')) {
            double value = parseExpression();

            if (!match(')')) {
                throw new IllegalArgumentException(
                        "Missing closing parenthesis");
            }

            return value;
        }

        if (position >= input.length()) {
            throw new IllegalArgumentException("Unexpected end of formula");
        }

        char current = peek();

        if (Character.isDigit(current) || current == '.') {
            return parseNumber();
        }

        if (Character.isLetter(current) || current == '_') {
            return parseIdentifier();
        }

        throw new IllegalArgumentException(
                "Unexpected character: " + current);
    }

    private double parseNumber() {
        int start = position;

        while (position < input.length()
                && (Character.isDigit(peek()) || peek() == '.')) {
            position++;
        }

        try {
            return Double.parseDouble(input.substring(start, position));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number");
        }
    }

    private double parseIdentifier() {
        skipSpaces();
        int start = position;

        while (position < input.length()
                && Character.isLetterOrDigit(peek())
                || position < input.length() && peek() == '_') {
            position++;
        }

        String name = input.substring(start, position);
        skipSpaces();

        if (match('(')) {
            return parseFunction(name);
        }

        if (context == null) {
            throw new IllegalArgumentException(
                    "A cell context is required for " + name);
        }

        Object value = context.getValue(name.toUpperCase(Locale.ROOT));

        if (value == null) {
            throw new IllegalArgumentException(
                    "Cell " + name + " has no value");
        }

        return toNumber(value);
    }

    private double parseFunction(String functionName) {
        String name = functionName.toUpperCase(Locale.ROOT);
        List<Double> values = new ArrayList<>();

        skipSpaces();

        if (match(')')) {
            return applyFunction(name, values);
        }

        while (true) {
            skipSpaces();

            // Support ranges such as A1:A5 in built-in functions.
            int savedPosition = position;
            String firstCell = tryReadCellReference();

            if (firstCell != null) {
                skipSpaces();

                if (match(':')) {
                    String lastCell = tryReadCellReference();

                    if (lastCell == null) {
                        throw new IllegalArgumentException(
                                "Invalid cell range");
                    }

                    values.addAll(readRange(firstCell, lastCell));
                } else {
                    position = savedPosition;
                    values.add(parseExpression());
                }
            } else {
                position = savedPosition;
                values.add(parseExpression());
            }

            skipSpaces();

            if (match(')')) {
                break;
            }

            if (!match(',')) {
                throw new IllegalArgumentException(
                        "Expected ',' or ')' in function");
            }
        }

        return applyFunction(name, values);
    }

    private String tryReadCellReference() {
        skipSpaces();
        int start = position;

        while (position < input.length()
                && Character.isLetter(peek())) {
            position++;
        }

        int lettersEnd = position;

        while (position < input.length()
                && Character.isDigit(peek())) {
            position++;
        }

        if (lettersEnd == start || position == lettersEnd) {
            position = start;
            return null;
        }

        return input.substring(start, position).toUpperCase(Locale.ROOT);
    }

    private List<Double> readRange(String first, String last) {
        int[] start = decodeReference(first);
        int[] end = decodeReference(last);

        if (start[0] > end[0] || start[1] > end[1]) {
            throw new IllegalArgumentException(
                    "Range must go from top-left to bottom-right");
        }

        List<Double> result = new ArrayList<>();

        for (int row = start[1]; row <= end[1]; row++) {
            for (int col = start[0]; col <= end[0]; col++) {
                String reference = encodeColumn(col) + row;
                Object value = context.getValue(reference);

                if (value != null) {
                    result.add(toNumber(value));
                }
            }
        }

        return result;
    }

    // Returns {column number, row number}; both are one-based.
    private int[] decodeReference(String reference) {
        int i = 0;
        int column = 0;

        while (i < reference.length()
                && Character.isLetter(reference.charAt(i))) {
            column = column * 26
                    + Character.toUpperCase(reference.charAt(i)) - 'A' + 1;
            i++;
        }

        int row = Integer.parseInt(reference.substring(i));
        return new int[] {column, row};
    }

    private String encodeColumn(int column) {
        StringBuilder result = new StringBuilder();

        while (column > 0) {
            column--;
            result.insert(0, (char) ('A' + column % 26));
            column /= 26;
        }

        return result.toString();
    }

    private double applyFunction(String name, List<Double> values) {
        switch (name) {
            case "SUM":
                return values.stream().mapToDouble(Double::doubleValue).sum();

            case "AVG":
            case "AVERAGE":
                requireValues(name, values);
                return values.stream().mapToDouble(Double::doubleValue)
                        .average().orElse(0);

            case "MIN":
                requireValues(name, values);
                return values.stream().mapToDouble(Double::doubleValue)
                        .min().orElse(0);

            case "MAX":
                requireValues(name, values);
                return values.stream().mapToDouble(Double::doubleValue)
                        .max().orElse(0);

            case "COUNT":
                return values.size();

            default:
                throw new IllegalArgumentException(
                        "Unknown function: " + name);
        }
    }

    private void requireValues(String name, List<Double> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    name + " requires at least one value");
        }
    }

    private double toNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Value is not numeric: " + value);
        }
    }

    private void skipSpaces() {
        while (position < input.length()
                && Character.isWhitespace(peek())) {
            position++;
        }
    }

    private boolean match(char expected) {
        if (position < input.length() && peek() == expected) {
            position++;
            return true;
        }

        return false;
    }

    private char peek() {
        return input.charAt(position);
    }
}
