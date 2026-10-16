package worklet1.commands;

import api.Spreadsheet;

public class SetCellCommand implements Command {

    private final Spreadsheet spreadsheet;
    private final String reference;
    private final String newContent;

    private final String oldContent;

    public SetCellCommand(
            Spreadsheet spreadsheet,
            String reference,
            String newContent) {

        this.spreadsheet = spreadsheet;
        this.reference = reference.toUpperCase();
        this.newContent = newContent;

        this.oldContent =
                spreadsheet.getAllCellContents()
                        .get(this.reference);
    }

    @Override
    public void execute() {
        spreadsheet.setCell(reference, newContent);
    }

    @Override
    public void undo() {
        spreadsheet.setCell(
                reference,
                oldContent == null ? "" : oldContent);
    }
}