
package worklet1.commands;

import api.Cell;
import api.Spreadsheet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import worklet1.formula.FormulaCell;
import worklet1.model.BasicCell;

/**
 * Treats an entire multi-cell paste as one undoable command.
 */
public class PasteCellsCommand implements Command {

    private final Spreadsheet spreadsheet;
    private final Map<String, String> newContents;
    private final Map<String, String> oldContents =
            new LinkedHashMap<>();

    public PasteCellsCommand(
            Spreadsheet spreadsheet,
            Map<String, String> newContents) {

        if (spreadsheet == null || newContents == null) {
            throw new IllegalArgumentException(
                    "Spreadsheet and pasted contents must not be null");
        }

        if (newContents.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nothing to paste");
        }

        this.spreadsheet = spreadsheet;
        this.newContents = new LinkedHashMap<>(newContents);

        for (Map.Entry<String, String> entry
                : this.newContents.entrySet()) {

            String reference = Objects.requireNonNull(
                    entry.getKey(), "Cell reference must not be null");

            String content = Objects.requireNonNull(
                    entry.getValue(), "Cell content must not be null");

            Cell oldCell = spreadsheet.getCell(reference);
            oldContents.put(reference, extractContent(oldCell));
        }
    }

    private String extractContent(Cell cell) {
        if (cell == null) {
            return "";
        }

        if (cell instanceof FormulaCell) {
            String formula = ((FormulaCell) cell).getFormula();
            return formula.startsWith("=")
                    ? formula
                    : "=" + formula;
        }

        if (cell instanceof BasicCell) {
            return ((BasicCell) cell).getContent();
        }

        return cell.getDisplayValue();
    }

    @Override
    public void execute() {
        for (Map.Entry<String, String> entry
                : newContents.entrySet()) {
            spreadsheet.setCell(
                    entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<String, String> entry
                : oldContents.entrySet()) {
            spreadsheet.setCell(
                    entry.getKey(), entry.getValue());
        }
    }
}
