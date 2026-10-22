
package worklet1.commands;

import api.Cell;
import api.Spreadsheet;
import worklet1.formula.FormulaCell;
import worklet1.model.BasicCell;

public class SetCellCommand implements Command {

    private final Spreadsheet spreadsheet;
    private final String reference;
    private final String newContent;
    private final String oldContent;

    public SetCellCommand(
            Spreadsheet spreadsheet,
            String reference,
            String newContent) {

        if (spreadsheet == null) {
            throw new IllegalArgumentException(
                    "Spreadsheet must not be null");
        }

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Cell reference must not be empty");
        }

        if (newContent == null) {
            throw new IllegalArgumentException(
                    "Cell content must not be null");
        }

        this.spreadsheet = spreadsheet;
        this.reference = reference.toUpperCase();
        this.newContent = newContent;

        Cell oldCell = spreadsheet.getCell(this.reference);
        this.oldContent = extractContent(oldCell);
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
        spreadsheet.setCell(reference, newContent);
    }

    @Override
    public void undo() {
        spreadsheet.setCell(reference, oldContent);
    }
}
