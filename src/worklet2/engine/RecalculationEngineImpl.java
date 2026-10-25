
package worklet2.engine;

import worklet2.graph.DependencyGraph;
import api.EvaluationContext;
import api.RecalculationEngine;
import api.Spreadsheet;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;

import worklet1.formula.FormulaCell;

public class RecalculationEngineImpl implements RecalculationEngine {

    private final Spreadsheet sheet;
    private final DependencyGraph dependencyGraph;
    private final Map<String, FormulaCell> formulaCells = new HashMap<>();
    private final EvaluationContext context;

    public RecalculationEngineImpl(Spreadsheet sheet) {
        this.sheet = Objects.requireNonNull(sheet, "Spreadsheet must not be null");
        this.dependencyGraph = new DependencyGraph();

        this.context = reference -> {
            String ref = normalizeReference(reference);

            FormulaCell formula = formulaCells.get(ref);

            if (formula != null) {
                return formula.getValue();
            }

            return this.sheet.getValue(ref);
        };
    }

    /**
     * Sets or updates a cell.
     *
     * Formula changes are validated against a temporary graph first.
     * If the new dependencies would create a cycle, the old formula
     * and its dependency relationships remain unchanged.
     */
    public void setCell(String reference, String content) {
        String ref = normalizeReference(reference);
        Objects.requireNonNull(content, "Cell content must not be null");

        if (content.startsWith("=")) {
            setFormulaCell(ref, content);
        } else {
            setLiteralCell(ref, content);
        }

        recalculate(ref);
    }

    private void setFormulaCell(String ref, String content) {
        // Build the candidate without changing the current formula.
        FormulaCell candidate = new FormulaCell(ref, content);

        // Atomically validate and replace this cell's incoming edges.
        // If validation fails, formulaCells and the original graph
        // remain unchanged.
        dependencyGraph.replaceDependencies(
                ref,
                candidate.getDependencies()
        );

        // Commit the formula only after the graph accepts it.
        formulaCells.put(ref, candidate);
    }

    private void setLiteralCell(String ref, String content) {
        /*
         * Let the spreadsheet validate/store the literal first.
         * If this throws, the graph and formula map are unchanged.
         */
        sheet.setCell(ref, content);

        // A literal cell no longer has formula dependencies.
        dependencyGraph.replaceDependencies(
                ref,
                Collections.emptySet()
        );

        formulaCells.remove(ref);
        dependencyGraph.addCell(ref);
    }

    /**
     * Recalculates all formula cells affected by the specified cell.
     */
    @Override
    public void recalculate(String cellReference) {
        String changed = normalizeReference(cellReference);

        Set<String> affected =
                dependencyGraph.findAffectedCells(changed);

        if (formulaCells.containsKey(changed)) {
            affected.add(changed);
        }

        if (affected.isEmpty()) {
            return;
        }

        var calculationOrder =
                dependencyGraph.topologicalSort(affected);

        for (String ref : calculationOrder) {
            FormulaCell formula = formulaCells.get(ref);

            if (formula == null) {
                continue;
            }

            System.out.println("Recalculating " + ref);
            formula.evaluate(context);
        }
    }

    /**
     * Returns the current value of a cell.
     */
    public Object getValue(String reference) {
        String ref = normalizeReference(reference);

        FormulaCell formula = formulaCells.get(ref);

        if (formula != null) {
            return formula.getValue();
        }

        return sheet.getValue(ref);
    }

    /**
     * Returns the display value, including formula error messages.
     */
    public String getDisplayValue(String reference) {
        String ref = normalizeReference(reference);

        FormulaCell formula = formulaCells.get(ref);

        if (formula != null) {
            return formula.getDisplayValue();
        }

        return sheet.getDisplayValue(ref);
    }

    /**
     * Returns the error associated with a cell, if any.
     */
    public String getError(String reference) {
        String ref = normalizeReference(reference);

        FormulaCell formula = formulaCells.get(ref);

        if (formula != null) {
            return formula.getError();
        }

        return sheet.getError(ref);
    }

    /**
     * Returns the dependency graph for testing and debugging.
     */
    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    private String normalizeReference(String reference) {
        Objects.requireNonNull(
                reference,
                "Cell reference must not be null"
        );

        String ref = reference.trim().toUpperCase(Locale.ROOT);

        if (!ref.matches("[A-Z]+[1-9][0-9]*")) {
            throw new IllegalArgumentException(
                    "Invalid cell reference: " + reference
            );
        }

        return ref;
    }
}
