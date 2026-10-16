package worklet1.formula;

import java.util.Set;
import java.util.TreeSet;

import worklet1.formula.ExpressionNode.*;

public class CellDependencyExtractor {

    public Set<String> extract(ExpressionNode expression) {
        Set<String> dependencies = new TreeSet<>();
        visit(expression, dependencies);
        return dependencies;
    }

    private void visit(
            ExpressionNode node,
            Set<String> dependencies) {

        if (node instanceof CellReferenceNode ref) {
            dependencies.add(
                    ref.reference().toUpperCase());
        }

        else if (node instanceof UnaryNode unary) {
            visit(unary.operand(), dependencies);
        }

        else if (node instanceof BinaryNode binary) {
            visit(binary.left(), dependencies);
            visit(binary.right(), dependencies);
        }

        else if (node instanceof FunctionCallNode function) {
            for (ExpressionNode argument : function.arguments()) {
                visit(argument, dependencies);
            }
        }

        else if (node instanceof RangeNode range) {
            addRange(range, dependencies);
        }

        // LiteralNode contains no cell references.
    }

    private void addRange(
            RangeNode range,
            Set<String> dependencies) {

        String start = range.startReference();
        String end = range.endReference();

        int startColumn = columnNumber(start);
        int endColumn = columnNumber(end);

        int startRow = rowNumber(start);
        int endRow = rowNumber(end);

        int minColumn = Math.min(startColumn, endColumn);
        int maxColumn = Math.max(startColumn, endColumn);

        int minRow = Math.min(startRow, endRow);
        int maxRow = Math.max(startRow, endRow);

        for (int column = minColumn;
             column <= maxColumn; column++) {

            for (int row = minRow;
                 row <= maxRow; row++) {

                dependencies.add(
                        columnName(column) + row);
            }
        }
    }

    private int columnNumber(String reference) {
        int result = 0;

        for (int i = 0; i < reference.length(); i++) {
            char ch = Character.toUpperCase(reference.charAt(i));

            if (!Character.isLetter(ch)) {
                break;
            }

            result = result * 26 + (ch - 'A' + 1);
        }

        return result;
    }

    private int rowNumber(String reference) {
        int i = 0;

        while (i < reference.length()
                && Character.isLetter(reference.charAt(i))) {
            i++;
        }

        return Integer.parseInt(reference.substring(i));
    }

    private String columnName(int number) {
        StringBuilder result = new StringBuilder();

        while (number > 0) {
            number--;

            result.insert(
                    0,
                    (char) ('A' + number % 26));

            number /= 26;
        }

        return result.toString();
    }
}