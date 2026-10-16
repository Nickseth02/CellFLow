
package worklet1.formula;

import api.Cell;
import api.EvaluationContext;
import java.util.Set;
import worklet1.ExpressionEvaluator;
import worklet1.parser.FormulaParseException;
import worklet1.parser.FormulaParser;

public class FormulaCell implements Cell {

    private final String reference;
    private String formula;

    private final FormulaParser parser;
    private final CellDependencyExtractor dependencyExtractor;

    private ExpressionNode expression;
    private Set<String> dependencies;

    private Object value;
    private String error;

    public FormulaCell(String reference, String formula) {
        this.reference = reference.toUpperCase();
        this.parser = new FormulaParser();
        this.dependencyExtractor = new CellDependencyExtractor();

        setFormula(formula);
    }

    public void setFormula(String formula) {
        this.formula = formula;
        this.value = null;
        this.error = null;

        try {
            this.expression = parser.parse(formula);
            this.dependencies =
                    dependencyExtractor.extract(expression);
        } catch (FormulaParseException e) {
            this.expression = null;
            this.dependencies = Set.of();
            this.error = "#PARSE!";
        }
    }

    @Override
    public String getReference() {
        return reference;
    }

    public String getFormula() {
        return formula;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getDisplayValue() {
        if (error != null) {
            return error;
        }

        return value == null ? "" : value.toString();
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public Set<String> getDependencies() {
        return Set.copyOf(dependencies);
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        if (expression == null) {
            value = null;
            return null;
        }

        try {
            value = ExpressionEvaluator.evaluate(formula, context);
            error = null;
            return value;

        } catch (ArithmeticException e) {
            value = null;
            error = "#DIV/0!";
            return null;

        } catch (RuntimeException e) {
            value = null;
            error = "#VALUE!";
            return null;
        }
    }
}
