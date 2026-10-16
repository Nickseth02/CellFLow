
package worklet1;

import api.Cell;
import api.EvaluationContext;
import java.util.Collections;
import java.util.Set;

public class BasicCell implements Cell {

    private String reference;
    private Object value;
    private String content;
    private String error;

    public BasicCell(String reference, Object value, String content) {
        this.reference = reference;
        this.value = value;
        this.content = content;
        this.error = null;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public String getDisplayValue() {
        if (error != null) {
            return "#ERROR!";
        }

        return value == null ? "" : value.toString();
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        if (content == null || content.trim().isEmpty()) {
            error = null;
            return value;
        }

        String expression = content.trim();

        // Only evaluate formulas beginning with '='.
        if (!expression.startsWith("=")) {
            error = null;
            return value;
        }

        try {
            Object result = ExpressionEvaluator.evaluate(
                expression.substring(1),
                context
            );

            value = result;
            error = null;
            return result;

        } catch (RuntimeException e) {
            error = e.getMessage() != null
                ? e.getMessage()
                : "Invalid formula";
            value = null;
            return null;
        }
    }

    public String getContent() {
        return content;
    }

    public void setValue(Object value) {
        this.value = value;
        this.error = null;
    }

    public void setContent(String content) {
        this.content = content;
        this.error = null;
    }
}

