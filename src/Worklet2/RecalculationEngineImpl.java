package Worklet2;

import api.EvaluationContext;
import api.RecalculationEngine;
import api.Spreadsheet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import worklet1.formula.FormulaCell;

public class RecalculationEngineImpl
        implements RecalculationEngine {

    // Spreadsheet containing literal and empty cells
    private final Spreadsheet sheet;

    // Dependency graph used for Worklet 2
    private final DependencyGraph dependencyGraph;

    // Stores all formula cells
    private final Map<String, FormulaCell> formulaCells =
            new HashMap<>();

    // Context used by FormulaCell when evaluating formulas
    private final EvaluationContext context;

    /*
     * Constructor
     */
    public RecalculationEngineImpl(Spreadsheet sheet) {

        this.sheet = sheet;

        this.dependencyGraph =
                new DependencyGraph();

        /*
         * IMPORTANT:
         * Context is initialized here, after sheet has
         * already been initialized.
         *
         * This fixes the Java error:
         * "variable sheet might not have been initialized"
         */
        this.context = reference -> {

            String ref =
                    reference.toUpperCase();

            /*
             * If the referenced cell is a formula,
             * return its calculated value.
             */
            FormulaCell formula =
                    formulaCells.get(ref);

            if (formula != null) {
                return formula.getValue();
            }

            /*
             * Otherwise get the value from the
             * normal spreadsheet model.
             */
            return this.sheet.getValue(ref);
        };
    }

    /*
     * Set or update a cell.
     *
     * Examples:
     *
     * setCell("A1", "10");
     * setCell("B1", "=A1+5");
     */
    public void setCell(
            String reference,
            String content) {

        String ref =
                reference.toUpperCase();

        /*
         * If this cell previously contained a formula,
         * remove its old dependency relationships.
         */
        removeOldDependencies(ref);

        /*
         * Formula cell
         */
        if (content.startsWith("=")) {

            FormulaCell formula =
                    new FormulaCell(ref, content);

            formulaCells.put(ref, formula);

            dependencyGraph.addCell(ref);

            /*
             * Add dependency edges.
             *
             * Example:
             *
             * B1 = =A1+5
             *
             * creates:
             *
             * A1 -> B1
             */
            for (String dependency :
                    formula.getDependencies()) {

                dependencyGraph.addDependency(
                        dependency,
                        ref
                );
            }

        } else {

            /*
             * Normal/literal cell.
             */
            formulaCells.remove(ref);

            sheet.setCell(ref, content);

            dependencyGraph.addCell(ref);
        }

        /*
         * Recalculate cells affected by this change.
         */
        recalculate(ref);
    }

    /*
     * Remove dependency edges belonging to
     * the previous formula in this cell.
     *
     * Example:
     *
     * B1 = =A1+5
     *
     * A1 -> B1
     *
     * If B1 changes to:
     *
     * B1 = =D1+5
     *
     * we remove:
     *
     * A1 -> B1
     *
     * and later add:
     *
     * D1 -> B1
     */
    private void removeOldDependencies(
            String ref) {

        FormulaCell old =
                formulaCells.get(ref);

        if (old == null) {
            return;
        }

        for (String dependency :
                old.getDependencies()) {

            dependencyGraph.removeDependency(
                    dependency,
                    ref
            );
        }
    }

    /*
     * Recalculate all cells affected by a change.
     *
     * Example:
     *
     * A1 -> B1 -> C1 -> D1
     *
     * If A1 changes:
     *
     * B1, C1 and D1
     * are recalculated.
     */
    @Override
    public void recalculate(
            String cellReference) {

        String changed =
                cellReference.toUpperCase();

        /*
         * Find all cells that depend directly
         * or indirectly on the changed cell.
         */
        Set<String> affected =
                dependencyGraph
                        .findAffectedCells(changed);

        /*
         * If the changed cell itself is a formula,
         * it also needs to be recalculated.
         */
        if (formulaCells.containsKey(changed)) {

            affected.add(changed);
        }

        /*
         * Nothing to recalculate.
         */
        if (affected.isEmpty()) {
            return;
        }

        /*
         * Calculate the cells in dependency order.
         *
         * Example:
         *
         * B1 -> C1 -> D1
         *
         * becomes:
         *
         * B1, C1, D1
         */
        var calculationOrder =
                dependencyGraph.topologicalSort(
                        affected
                );

        /*
         * Evaluate each formula in the
         * correct order.
         */
        for (String ref :
                calculationOrder) {

            FormulaCell formula =
                    formulaCells.get(ref);

            /*
             * Affected set should normally contain
             * only formula cells, but this check makes
             * the engine safer.
             */
            if (formula == null) {
                continue;
            }

            System.out.println(
                    "Recalculating " + ref
            );

            formula.evaluate(context);
        }
    }

    /*
     * Get the current value of a cell.
     */
    public Object getValue(
            String reference) {

        String ref =
                reference.toUpperCase();

        /*
         * Formula cell
         */
        FormulaCell formula =
                formulaCells.get(ref);

        if (formula != null) {
            return formula.getValue();
        }

        /*
         * Literal or empty cell
         */
        return sheet.getValue(ref);
    }

    /*
     * Get the display value of a cell.
     *
     * For example:
     *
     * B1 = 15
     *
     * returns:
     *
     * "15"
     *
     * If there is an error:
     *
     * "#DIV/0!"
     */
    public String getDisplayValue(
            String reference) {

        String ref =
                reference.toUpperCase();

        /*
         * Formula cell
         */
        FormulaCell formula =
                formulaCells.get(ref);

        if (formula != null) {
            return formula.getDisplayValue();
        }

        /*
         * Literal or empty cell
         */
        return sheet.getDisplayValue(ref);
    }

    /*
     * Get the dependency graph.
     *
     * Useful for testing and debugging.
     */
    public DependencyGraph getDependencyGraph() {

        return dependencyGraph;
    }
}