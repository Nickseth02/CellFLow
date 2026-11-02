
package worklet3.ui;

import api.Cell;
import api.Spreadsheet;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import worklet1.commands.PasteCellsCommand;
import worklet1.commands.SetCellCommand;
import worklet1.commands.UndoRedoManager;
import worklet1.formula.FormulaCell;
import worklet1.model.BasicCell;
import worklet3.persistence.SheetLoader;
import worklet3.persistence.SheetSerializer;

public final class SpreadsheetUI extends JFrame {

    private static final int ROWS = 50;
    private static final int COLUMNS = 26;

    private static final Pattern CELL_REFERENCE_PATTERN =
            Pattern.compile(
                "(?<![A-Za-z0-9_])(\\$?)([A-Za-z]+)(\\$?)([1-9][0-9]*)(?![A-Za-z0-9_])"
            );

    private final Spreadsheet spreadsheet;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel cellLabel;
    private final JLabel statusLabel;

    private final Map<String, String> originalContents =
            new LinkedHashMap<>();

    private final SheetSerializer serializer = new SheetSerializer();
    private final SheetLoader loader = new SheetLoader();

    private UndoRedoManager undoRedoManager = new UndoRedoManager();

    private boolean refreshing;

    private String[][] copiedCells;
    private int copiedRows;
    private int copiedColumns;
    private int copiedStartRow;
    private int copiedStartColumn;

    public SpreadsheetUI(Spreadsheet spreadsheet) {
        super("Object-Oriented Spreadsheet Engine");

        if (spreadsheet == null) {
            throw new IllegalArgumentException(
                    "Spreadsheet must not be null");
        }

        this.spreadsheet = spreadsheet;

        String[] columnNames = new String[COLUMNS];

        for (int i = 0; i < COLUMNS; i++) {
            columnNames[i] = columnName(i + 1);
        }

        tableModel = new DefaultTableModel(columnNames, ROWS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }

            @Override
            public void setValueAt(Object value, int row, int column) {
                if (refreshing) {
                    super.setValueAt(value, row, column);
                    return;
                }

                String reference =
                        columnName(column + 1) + (row + 1);

                String content = value == null
                        ? ""
                        : value.toString();

                super.setValueAt(value, row, column);
                commitCell(reference, content);
            }
        };

        table = new JTable(tableModel);
        table.setCellSelectionEnabled(true);

        table.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setShowGrid(true);
        table.setGridColor(new Color(160, 174, 190));
        table.setRowHeight(24);
        table.setSelectionBackground(new Color(190, 215, 240));
        table.setSelectionForeground(Color.BLACK);

        table.putClientProperty(
                "terminateEditOnFocusLost", Boolean.TRUE);

        for (int i = 0; i < COLUMNS; i++) {
            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(100);
        }

        cellLabel = new JLabel("Cell: A1");
        statusLabel = new JLabel("Ready.");

        JButton undoButton = new JButton("Undo");
        JButton redoButton = new JButton("Redo");
        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");

        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        saveButton.addActionListener(e -> saveSheet());
        loadButton.addActionListener(e -> loadSheet());

        JPanel toolbar = new JPanel(
                new FlowLayout(FlowLayout.LEFT));

        toolbar.add(cellLabel);
        toolbar.add(undoButton);
        toolbar.add(redoButton);
        toolbar.add(saveButton);
        toolbar.add(loadButton);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedCellLabel();
            }
        });

        table.getColumnModel()
                .getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        updateSelectedCellLabel();
                    }
                });

        installCopyPasteShortcuts();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);

        refreshGrid();
        updateSelectedCellLabel();
    }

    private void commitCell(String reference, String content) {
        try {
            undoRedoManager.execute(
                    new SetCellCommand(
                            spreadsheet, reference, content));

            if (content.isEmpty()) {
                originalContents.remove(reference);
            } else {
                originalContents.put(reference, content);
            }

            refreshGrid();
            statusLabel.setText("Updated " + reference);

        } catch (RuntimeException exception) {
            statusLabel.setText(
                    "Error: " + exception.getMessage());
            refreshGrid();
        }

        updateSelectedCellLabel();
    }

    private void undo() {
        try {
            undoRedoManager.undo();
            rebuildTrackedContents();
            refreshGrid();
            statusLabel.setText("Undo completed.");

        } catch (RuntimeException exception) {
            statusLabel.setText(
                    "Undo failed: " + exception.getMessage());
        }
    }

    private void redo() {
        try {
            undoRedoManager.redo();
            rebuildTrackedContents();
            refreshGrid();
            statusLabel.setText("Redo completed.");

        } catch (RuntimeException exception) {
            statusLabel.setText(
                    "Redo failed: " + exception.getMessage());
        }
    }

    private void saveSheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Spreadsheet");

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();

        if (!path.toString().toLowerCase().endsWith(".sheet")) {
            path = Path.of(path.toString() + ".sheet");
        }

        try {
            serializer.save(
                    new LinkedHashMap<>(originalContents), path);

            statusLabel.setText(
                    "Saved: " + path.getFileName());

        } catch (IOException | RuntimeException exception) {
            showError("Could not save spreadsheet", exception);
        }
    }

    private void loadSheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Spreadsheet");

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();

        try {
            Map<String, String> loadedContents = loader.load(path);

            for (String reference : loadedContents.keySet()) {
                int[] position = cellPosition(reference);

                if (position[0] < 0 || position[0] >= ROWS
                        || position[1] < 0
                        || position[1] >= COLUMNS) {

                    throw new IOException(
                            "Cell is outside the visible grid: "
                                    + reference);
                }
            }

            for (String reference : originalContents.keySet()) {
                spreadsheet.setCell(reference, "");
            }

            for (Map.Entry<String, String> entry
                    : loadedContents.entrySet()) {

                spreadsheet.setCell(
                        entry.getKey(), entry.getValue());
            }

            originalContents.clear();
            originalContents.putAll(loadedContents);

            undoRedoManager = new UndoRedoManager();

            refreshGrid();
            statusLabel.setText(
                    "Loaded: " + path.getFileName());

        } catch (IOException | RuntimeException exception) {
            showError("Could not load spreadsheet", exception);
            refreshGrid();
        }
    }

    private void installCopyPasteShortcuts() {
        InputMap inputMap =
                table.getInputMap(JComponent.WHEN_FOCUSED);

        ActionMap actionMap = table.getActionMap();

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
                "copy-cell");

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK),
                "paste-cell");

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
                "undo-cell");

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK),
                "redo-cell");

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_BACK_SPACE, 0),
                "clear-cells");

        inputMap.put(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_DELETE, 0),
                "clear-cells");

        actionMap.put("copy-cell", new AbstractAction() {
            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e) {
                copySelectedRange();
            }
        });

        actionMap.put("paste-cell", new AbstractAction() {
            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e) {
                pasteSelectedRange();
            }
        });

        actionMap.put("undo-cell", new AbstractAction() {
            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e) {
                undo();
            }
        });

        actionMap.put("redo-cell", new AbstractAction() {
            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e) {
                redo();
            }
        });

        actionMap.put("clear-cells", new AbstractAction() {
            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e) {
                clearSelectedRange();
            }
        });
    }

    private void copySelectedRange() {
        int[] selectedRows = table.getSelectedRows();
        int[] selectedColumns = table.getSelectedColumns();

        if (selectedRows.length == 0
                || selectedColumns.length == 0) {
            statusLabel.setText("Select cells to copy.");
            return;
        }

        copiedStartRow = selectedRows[0];
        copiedStartColumn = selectedColumns[0];

        for (int row : selectedRows) {
            copiedStartRow = Math.min(copiedStartRow, row);
        }

        for (int column : selectedColumns) {
            copiedStartColumn =
                    Math.min(copiedStartColumn, column);
        }

        int lastRow = selectedRows[selectedRows.length - 1];
        int lastColumn =
                selectedColumns[selectedColumns.length - 1];

        copiedRows = lastRow - copiedStartRow + 1;
        copiedColumns = lastColumn - copiedStartColumn + 1;

        copiedCells = new String[copiedRows][copiedColumns];

        for (int r = 0; r < copiedRows; r++) {
            for (int c = 0; c < copiedColumns; c++) {
                String reference =
                        columnName(copiedStartColumn + c + 1)
                                + (copiedStartRow + r + 1);

                copiedCells[r][c] =
                        getOriginalContent(reference);
            }
        }

        statusLabel.setText(
                "Copied " + copiedRows + " x "
                        + copiedColumns + " cell range.");
    }

    private void pasteSelectedRange() {
        if (copiedCells == null) {
            statusLabel.setText("Copy cells before pasting.");
            return;
        }

        int targetRow = table.getSelectedRow();
        int targetColumn = table.getSelectedColumn();

        if (targetRow < 0 || targetColumn < 0) {
            statusLabel.setText("Select a destination cell.");
            return;
        }

        if (targetRow + copiedRows > ROWS
                || targetColumn + copiedColumns > COLUMNS) {

            statusLabel.setText(
                    "Paste cancelled: range exceeds spreadsheet boundary.");
            return;
        }

        int rowOffset = targetRow - copiedStartRow;
        int columnOffset =
                targetColumn - copiedStartColumn;

        Map<String, String> pastedContents =
                new LinkedHashMap<>();

        for (int r = 0; r < copiedRows; r++) {
            for (int c = 0; c < copiedColumns; c++) {

                String destination =
                        columnName(targetColumn + c + 1)
                                + (targetRow + r + 1);

                String content = copiedCells[r][c];

                if (content.startsWith("=")) {
                    content = adjustFormulaReferences(
                            content, rowOffset, columnOffset);
                }

                pastedContents.put(destination, content);
            }
        }

        try {
            undoRedoManager.execute(
                    new PasteCellsCommand(
                            spreadsheet, pastedContents));

            for (Map.Entry<String, String> entry
                    : pastedContents.entrySet()) {

                if (entry.getValue().isEmpty()) {
                    originalContents.remove(entry.getKey());
                } else {
                    originalContents.put(
                            entry.getKey(), entry.getValue());
                }
            }

            refreshGrid();

            statusLabel.setText(
                    "Pasted " + copiedRows + " x "
                            + copiedColumns
                            + " cells. Undo is available.");

        } catch (RuntimeException exception) {
            statusLabel.setText(
                    "Paste failed: " + exception.getMessage());
            refreshGrid();
        }
    }

    private void clearSelectedRange() {
        int[] selectedRows = table.getSelectedRows();
        int[] selectedColumns = table.getSelectedColumns();

        if (selectedRows.length == 0
                || selectedColumns.length == 0) {
            statusLabel.setText("Select cells to delete.");
            return;
        }

        Map<String, String> clearedContents =
                new LinkedHashMap<>();

        for (int row : selectedRows) {
            for (int column : selectedColumns) {
                String reference =
                        columnName(column + 1) + (row + 1);

                clearedContents.put(reference, "");
            }
        }

        try {
            undoRedoManager.execute(
                    new PasteCellsCommand(
                            spreadsheet, clearedContents));

            for (String reference : clearedContents.keySet()) {
                originalContents.remove(reference);
            }

            refreshGrid();

            statusLabel.setText(
                    "Cleared " + clearedContents.size()
                            + " cells. Undo is available.");

        } catch (RuntimeException exception) {
            statusLabel.setText(
                    "Delete failed: " + exception.getMessage());
            refreshGrid();
        }
    }

    private String adjustFormulaReferences(
            String formula, int rowOffset, int columnOffset) {

        Matcher matcher =
                CELL_REFERENCE_PATTERN.matcher(formula);

        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String originalReference = matcher.group();

            boolean absoluteColumn =
                    !matcher.group(1).isEmpty();

            String columnText = matcher.group(2);

            boolean absoluteRow =
                    !matcher.group(3).isEmpty();

            int rowNumber =
                    Integer.parseInt(matcher.group(4));

            int columnNumber = columnIndex(columnText);

            int newColumn = absoluteColumn
                    ? columnNumber
                    : columnNumber + columnOffset;

            int newRow = absoluteRow
                    ? rowNumber
                    : rowNumber + rowOffset;

            if (newColumn < 1 || newRow < 1) {
                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(originalReference));
                continue;
            }

            String adjustedReference =
                    (absoluteColumn ? "$" : "")
                            + columnName(newColumn)
                            + (absoluteRow ? "$" : "")
                            + newRow;

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(adjustedReference));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private int columnIndex(String columnName) {
        int result = 0;

        for (int i = 0; i < columnName.length(); i++) {
            char ch = Character.toUpperCase(columnName.charAt(i));
            result = result * 26 + (ch - 'A' + 1);
        }

        return result;
    }

    private String getOriginalContent(String reference) {
        if (originalContents.containsKey(reference)) {
            return originalContents.get(reference);
        }

        return extractContent(spreadsheet.getCell(reference));
    }

    private String extractContent(Cell cell) {
        if (cell == null) {
            return "";
        }

        if (cell instanceof FormulaCell) {
            String formula =
                    ((FormulaCell) cell).getFormula();

            return formula.startsWith("=")
                    ? formula
                    : "=" + formula;
        }

        if (cell instanceof BasicCell) {
            return ((BasicCell) cell).getContent();
        }

        return cell.getDisplayValue();
    }

    private void rebuildTrackedContents() {
        originalContents.clear();

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {

                String reference =
                        columnName(column + 1) + (row + 1);

                String content = extractContent(
                        spreadsheet.getCell(reference));

                if (!content.isEmpty()) {
                    originalContents.put(reference, content);
                }
            }
        }
    }

    private void refreshGrid() {
        refreshing = true;

        try {
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {

                    String reference =
                            columnName(column + 1) + (row + 1);

                    String display =
                            spreadsheet.getDisplayValue(reference);

                    tableModel.setValueAt(
                            display == null ? "" : display,
                            row,
                            column);
                }
            }
        } finally {
            refreshing = false;
        }
    }

    private void updateSelectedCellLabel() {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();

        if (row >= 0 && column >= 0) {
            cellLabel.setText(
                    "Cell: " + columnName(column + 1) + (row + 1));
        }
    }

    private static int[] cellPosition(String reference) {
        int index = 0;
        int column = 0;

        while (index < reference.length()
                && Character.isLetter(reference.charAt(index))) {

            column = column * 26
                    + Character.toUpperCase(reference.charAt(index))
                    - 'A' + 1;

            index++;
        }

        int row = Integer.parseInt(reference.substring(index));

        return new int[]{row - 1, column - 1};
    }

    private static String columnName(int column) {
        StringBuilder name = new StringBuilder();

        while (column > 0) {
            column--;
            name.insert(0, (char) ('A' + column % 26));
            column /= 26;
        }

        return name.toString();
    }

    private void showError(String message, Exception exception) {
        String detail = exception.getMessage() == null
                ? "Unknown error"
                : exception.getMessage();

        statusLabel.setText(message + ": " + detail);

        JOptionPane.showMessageDialog(
                this,
                detail,
                message,
                JOptionPane.ERROR_MESSAGE);
    }

    public static void launch(Spreadsheet spreadsheet) {
        SwingUtilities.invokeLater(() -> {
            SpreadsheetUI ui = new SpreadsheetUI(spreadsheet);
            ui.setVisible(true);
        });
    }
}
