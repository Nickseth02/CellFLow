package api;

import java.util.Set;

public interface Cell {
    String getReference();
    Object getValue();
    String getDisplayValue();
    String getError();
    Set<String> getDependencies();
    Object evaluate(EvaluationContext context);
}