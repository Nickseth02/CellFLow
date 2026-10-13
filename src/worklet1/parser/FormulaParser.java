
package worklet1.parser;

import java.util.ArrayList;
import java.util.List;
import worklet1.formula.ExpressionNode;
import worklet1.formula.ExpressionNode.BinaryNode;
import worklet1.formula.ExpressionNode.CellReferenceNode;
import worklet1.formula.ExpressionNode.FunctionCallNode;
import worklet1.formula.ExpressionNode.LiteralNode;
import worklet1.formula.ExpressionNode.RangeNode;
import worklet1.formula.ExpressionNode.UnaryNode;

public class FormulaParser {

    private List<Token> tokens;
    private int position;

    public ExpressionNode parse(String formula) {
        tokens = new Tokenizer().tokenize(formula);
        position = 0;

        ExpressionNode expression = parseComparison();

        if (!check(TokenType.EOF)) {
            throw error("Unexpected token '" + peek().getText() + "'");
        }

        return expression;
    }

    // Comparison: =, <>, <, >, <=, >=
    private ExpressionNode parseComparison() {
        ExpressionNode expression = parseConcatenation();

        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL,
                TokenType.LESS_THAN, TokenType.GREATER_THAN,
                TokenType.LESS_EQUAL, TokenType.GREATER_EQUAL)) {

            String operator = previous().getText();
            ExpressionNode right = parseConcatenation();

            expression = new BinaryNode(operator, expression, right);
        }

        return expression;
    }

    // Text concatenation using &
    private ExpressionNode parseConcatenation() {
        ExpressionNode expression = parseAddition();

        while (match(TokenType.CONCAT)) {
            String operator = previous().getText();
            ExpressionNode right = parseAddition();

            expression = new BinaryNode(operator, expression, right);
        }

        return expression;
    }

    private ExpressionNode parseAddition() {
        ExpressionNode expression = parseMultiplication();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            String operator = previous().getText();
            ExpressionNode right = parseMultiplication();

            expression = new BinaryNode(operator, expression, right);
        }

        return expression;
    }

    private ExpressionNode parseMultiplication() {
        ExpressionNode expression = parseUnary();

        while (match(TokenType.MULTIPLY, TokenType.DIVIDE)) {
            String operator = previous().getText();
            ExpressionNode right = parseUnary();

            expression = new BinaryNode(operator, expression, right);
        }

        return expression;
    }

    private ExpressionNode parseUnary() {
        if (match(TokenType.PLUS, TokenType.MINUS)) {
            String operator = previous().getText();
            return new UnaryNode(operator, parseUnary());
        }

        return parsePower();
    }

    private ExpressionNode parsePower() {
        ExpressionNode expression = parsePrimary();

        // Power is right-associative: 2^3^2 means 2^(3^2).
        if (match(TokenType.POWER)) {
            String operator = previous().getText();
            ExpressionNode right = parseUnary();

            expression = new BinaryNode(operator, expression, right);
        }

        return expression;
    }

    private ExpressionNode parsePrimary() {
        if (match(TokenType.NUMBER)) {
            double value = Double.parseDouble(previous().getText());
            return new LiteralNode(value);
        }

        if (match(TokenType.STRING)) {
            return new LiteralNode(previous().getText());
        }

        if (match(TokenType.IDENTIFIER)) {
            String name = previous().getText();

            if (match(TokenType.LEFT_PAREN)) {
                return parseFunctionCall(name);
            }

            if (name.equalsIgnoreCase("TRUE")) {
                return new LiteralNode(Boolean.TRUE);
            }

            if (name.equalsIgnoreCase("FALSE")) {
                return new LiteralNode(Boolean.FALSE);
            }

            if (isCellReference(name)) {
                if (match(TokenType.COLON)) {
                    Token end = consume(
                            TokenType.IDENTIFIER,
                            "Expected a cell reference after ':'");

                    if (!isCellReference(end.getText())) {
                        throw error("Invalid range endpoint");
                    }

                    return new RangeNode(name.toUpperCase(),
                            end.getText().toUpperCase());
                }

                return new CellReferenceNode(name.toUpperCase());
            }

            throw error("Unknown name '" + name + "'");
        }

        if (match(TokenType.LEFT_PAREN)) {
            ExpressionNode expression = parseComparison();
            consume(TokenType.RIGHT_PAREN,
                    "Expected ')' after expression");
            return expression;
        }

        throw error("Expected a number, reference, function or '('");
    }

    private ExpressionNode parseFunctionCall(String name) {
        List<ExpressionNode> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseComparison());
            } while (match(TokenType.COMMA));
        }

        consume(TokenType.RIGHT_PAREN,
                "Expected ')' after function arguments");

        return new FunctionCallNode(name, arguments);
    }

    private boolean isCellReference(String text) {
        return text.matches("(?i)[A-Z]+[1-9][0-9]*");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                position++;
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return tokens.get(position++);
        }

        throw error(message);
    }

    private Token peek() {
        return tokens.get(position);
    }

    private Token previous() {
        return tokens.get(position - 1);
    }

    private FormulaParseException error(String message) {
        return new FormulaParseException(
                message + " at position " + peek().getPosition());
    }
}
