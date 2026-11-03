
package worklet2.engine;

import worklet2.engine.error.ErrorHandler;
import worklet2.engine.error.CellError;
import worklet2.engine.error.CellErrorType;
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

/**
 * Worklet 2 recalculation engine.
 *
 * Handles formula evaluation, dependency-based recalculation,
 * error detection, error propagation and error recovery.
 */
public class RecalculationEngineImpl implements RecalculationEngine {

    private final Spreadsheet sheet;
    private final DependencyGraph dependencyGraph;

    private final Map<String, FormulaCell> formulaCells = new HashMap<>();

    private final ErrorHandler errorHandler = new ErrorHandler();

    private final EvaluationContext context;

    public RecalculationEngineImpl(Spreadsheet sheet) {

        this.sheet = Objects.requireNonNull(
                sheet,
                "Spreadsheet must not be null"
        );

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
     */
    public void setCell(String reference, String content) {

        String ref = normalizeReference(reference);

        Objects.requireNonNull(
                content,
                "Cell content must not be null"
        );

        if (content.startsWith("=")) {
            setFormulaCell(ref, content);
        } else {
            setLiteralCell(ref, content);
        }

        recalculate(ref);
    }

    /**
     * Sets a formula after validating its dependencies.
     */
    private void setFormulaCell(String ref, String content) {

        FormulaCell candidate = new FormulaCell(ref, content);

        dependencyGraph.replaceDependencies(
                ref,
                candidate.getDependencies()
        );

        formulaCells.put(ref, candidate);
    }

    /**
     * Replaces a formula with a literal value.
     */
    private void setLiteralCell(String ref, String content) {

        sheet.setCell(ref, content);

        dependencyGraph.replaceDependencies(
                ref,
                Collections.emptySet()
        );

        formulaCells.remove(ref);

        errorHandler.clearError(ref);

        dependencyGraph.addCell(ref);
    }

    /**
     * Recalculates all formula cells affected by a changed cell.
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

            /*
             * Check whether any dependency has an existing error.
             */
            CellError dependencyError =
                    errorHandler.findDependencyError(
                            dependencyGraph.getDependencies(ref)
                    );

            /*
             * Propagate the dependency error instead of evaluating
             * a formula whose input is already invalid.
             */
            if (dependencyError != null) {

                errorHandler.propagateError(
                        ref,
                        dependencyError
                );

                continue;
            }

            /*
             * Clear the previous Worklet 2 error before recalculation.
             */
            errorHandler.clearError(ref);

            /*
             * Evaluate the formula.
             */
            formula.evaluate(context);

            /*
             * Detect and record errors produced by FormulaCell.
             */
            String formulaError = formula.getError();

            if (formulaError != null) {

                CellErrorType errorType;

                switch (formulaError) {

                    case "#DIV/0!":
                        errorType =
                                CellErrorType.DIVISION_BY_ZERO;
                        break;

                    case "#PARSE!":
                        errorType =
                                CellErrorType.FORMULA_PARSE;
                        break;

                    default:
                        errorType =
                                CellErrorType.INVALID_VALUE;
                        break;
                }

                errorHandler.recordError(
                        ref,
                        errorType,
                        formulaError
                );
            }
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
     * Returns the display value, including propagated errors.
     */
    public String getDisplayValue(String reference) {

        String ref = normalizeReference(reference);

        String errorCode = errorHandler.getErrorCode(ref);

        if (errorCode != null) {
            return errorCode;
        }

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

        String errorCode = errorHandler.getErrorCode(ref);

        if (errorCode != null) {
            return errorCode;
        }

        FormulaCell formula = formulaCells.get(ref);

        if (formula != null) {
            return formula.getError();
        }

        return sheet.getError(ref);
    }

    /**
     * Returns the Worklet 2 error handler.
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Returns the dependency graph for testing and debugging.
     */
    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    /**
     * Normalizes and validates a cell reference.
     */
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
