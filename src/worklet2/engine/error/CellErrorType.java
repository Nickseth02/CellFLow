package worklet2.engine.error;

public enum CellErrorType {

    DIVISION_BY_ZERO("#DIV/0!"),
    INVALID_VALUE("#VALUE!"),
    FORMULA_PARSE("#PARSE!"),
    CIRCULAR_DEPENDENCY("#CIRCULAR!");

    private final String displayCode;

    CellErrorType(String displayCode) {
        this.displayCode = displayCode;
    }

    public String getDisplayCode() {
        return displayCode;
    }
}