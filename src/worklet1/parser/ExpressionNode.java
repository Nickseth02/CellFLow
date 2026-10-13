
package worklet1.formula;

import java.util.List;

public interface ExpressionNode {

    record LiteralNode(Object value) implements ExpressionNode {
    }

    record CellReferenceNode(String reference) implements ExpressionNode {
    }

    record RangeNode(String startReference, String endReference)
            implements ExpressionNode {
    }

    record UnaryNode(String operator, ExpressionNode operand)
            implements ExpressionNode {
    }

    record BinaryNode(String operator,
                      ExpressionNode left,
                      ExpressionNode right) implements ExpressionNode {
    }

    record FunctionCallNode(String name, List<ExpressionNode> arguments)
            implements ExpressionNode {

        public FunctionCallNode {
            name = name.toUpperCase();
            arguments = List.copyOf(arguments);
        }
    }
}
