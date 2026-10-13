package api;

public interface Spreadsheet {
    void setCell(String reference, String content);
    Cell getCell(String reference);
    Object getValue(String reference);
    String getDisplayValue(String reference);
    String getError(String reference);
}