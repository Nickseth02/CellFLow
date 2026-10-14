
package worklet1;

import api.Cell;
import api.EvaluationContext;
import java.util.Collections;
import java.util.Set;

public class BasicCell implements Cell {

    private String reference;
    private Object value;
    private String content;

    public BasicCell(String reference, Object value, String content) {
        this.reference = reference;
        this.value = value;
        this.content = content;
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
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return value;
    }

    public String getContent() {
        return content;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
