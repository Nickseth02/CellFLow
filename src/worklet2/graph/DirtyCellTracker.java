package worklet2.graph;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Identifies cells that become dirty when a source cell changes.
 *
 * Uses Breadth-First Search (BFS) to find all direct and
 * indirect dependents.
 *
 * Does not perform formula evaluation or calculation ordering.
 */
public final class DirtyCellTracker {

    private final DependencyGraph dependencyGraph;

    public DirtyCellTracker(DependencyGraph dependencyGraph) {
        if (dependencyGraph == null) {
            throw new IllegalArgumentException(
                "Dependency graph cannot be null."
            );
        }

        this.dependencyGraph = dependencyGraph;
    }

    /**
     * Finds every cell affected by a change in the given cell.
     *
     * The changed cell itself is excluded.
     *
     * @param changedCell reference of the edited cell
     * @return unmodifiable set of affected cells
     */
    public Set<String> findDirtyCells(String changedCell) {

        Set<String> dirtyCells = new LinkedHashSet<>();
        Queue<String> pendingCells = new ArrayDeque<>();

        // Start from the edited cell.
        pendingCells.offer(changedCell.trim().toUpperCase());

        while (!pendingCells.isEmpty()) {

            String currentCell = pendingCells.poll();

            // Visit all direct dependents.
            for (String dependent :
                    dependencyGraph.getDependents(currentCell)) {

                // Add only cells that have not been visited.
                if (dirtyCells.add(dependent)) {
                    pendingCells.offer(dependent);
                }
            }
        }

        return Collections.unmodifiableSet(dirtyCells);
    }
}
