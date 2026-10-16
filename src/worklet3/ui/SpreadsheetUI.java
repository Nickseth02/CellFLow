package worklet3.ui;

import api.Cell;
import api.Spreadsheet;
import worklet3.persistence.SheetLoader;
import worklet3.persistence.SheetSerializer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spreadsheet UI for Worklet 3.
 * Adds Save and Load buttons while using the team's existing Spreadsheet API.
 *
 * Note: the current API does not expose all populated cell contents. This UI
 * therefore tracks edits made through this window. For complete persistence
 * across edits made elsewhere, add a method to the model to enumerate cells.
 */
public final class SpreadsheetUI extends JFrame {
    private static final int ROWS = 50;
    private static final int COLUMNS = 26;

    private final Spreadsheet spreadsheet;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel cellLabel;
    private final JLabel statusLabel;
    private final Map<String, String> originalContents =
            new LinkedHashMap<>();

    private final SheetSerializer serializer = new SheetSerializer();
    private final SheetLoader loader = new SheetLoader();
    private boolean refreshing;

    public SpreadsheetUI(Spreadsheet spreadsheet) {
        super("Object-Oriented Spreadsheet Engine");
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

                String reference = columnName(column + 1) + (row + 1);
                String content = value == null ? "" : value.toString();
                commitCell(reference, content);
            }
        };

        table = new JTable(tableModel);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(new Color(160, 174, 190));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowHeight(24);
        table.setSelectionBackground(new Color(190, 215, 240));
        table.setSelectionForeground(Color.BLACK);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        for (int i = 0; i < COLUMNS; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(100);
        }

        cellLabel = new JLabel("Cell: A1");
        statusLabel = new JLabel(
                "Ready. Select a cell and type to enter a value.");

        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");
        saveButton.addActionListener(event -> saveSpreadsheet());
        loadButton.addActionListener(event -> loadSpreadsheet());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(cellLabel);
        topPanel.add(saveButton);
        topPanel.add(loadButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateSelectedCellLabel();
            }
        });
        table.getColumnModel().getSelectionModel()
                .addListSelectionListener(event -> {
                    if (!event.getValueIsAdjusting()) {
                        updateSelectedCellLabel();
                    }
                });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);

        refreshGrid();
        updateSelectedCellLabel();
    }

    private void commitCell(String reference, String content) {
        try {
            // The API owns parsing/evaluation. The UI should not reject formulas.
            spreadsheet.setCell(reference, content);

            if (content.isEmpty()) {
                originalContents.remove(reference);
            } else {
                originalContents.put(reference, content);
            }

            refreshGrid();
            statusLabel.setText("Updated " + reference);
        } catch (RuntimeException exception) {
            statusLabel.setText("Error: "
                    + (exception.getMessage() == null
                    ? "Unable to update cell."
                    : exception.getMessage()));
            refreshGrid();
        }

        updateSelectedCellLabel();
    }

    private void saveSpreadsheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Spreadsheet");
        chooser.setSelectedFile(new java.io.File("spreadsheet.sheet"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        if (!file.getFileName().toString().toLowerCase()
                .endsWith(".sheet")) {
            file = file.resolveSibling(file.getFileName() + ".sheet");
        }

        try {
            serializer.save(originalContents, file);
            statusLabel.setText("Saved: " + file.getFileName());
        } catch (IOException | RuntimeException exception) {
            showError("Save failed", exception);
        }
    }

    private void loadSpreadsheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Spreadsheet");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();

        try {
            Map<String, String> loaded = loader.load(file);
            Map<String, String> previous =
                    new LinkedHashMap<>(originalContents);

            try {
                // Clear cells known to this UI, then restore loaded contents.
                for (String reference : previous.keySet()) {
                    spreadsheet.setCell(reference, "");
                }
                for (Map.Entry<String, String> entry : loaded.entrySet()) {
                    spreadsheet.setCell(entry.getKey(), entry.getValue());
                }

                originalContents.clear();
                originalContents.putAll(loaded);
                refreshGrid();
                statusLabel.setText("Loaded: " + file.getFileName());
            } catch (RuntimeException exception) {
                // Best-effort rollback to the state tracked before loading.
                try {
                    for (String reference : loaded.keySet()) {
                        spreadsheet.setCell(reference, "");
                    }
                    for (Map.Entry<String, String> entry : previous.entrySet()) {
                        spreadsheet.setCell(entry.getKey(), entry.getValue());
                    }
                    originalContents.clear();
                    originalContents.putAll(previous);
                    refreshGrid();
                } catch (RuntimeException rollbackError) {
                    statusLabel.setText(
                            "Load failed; rollback was incomplete.");
                }
                throw exception;
            }
        } catch (IOException | RuntimeException exception) {
            showError("Load failed", exception);
        }
    }

    private void showError(String title, Exception exception) {
        String message = exception.getMessage() == null
                ? "An unexpected error occurred."
                : exception.getMessage();
        statusLabel.setText(title + ": " + message);
        JOptionPane.showMessageDialog(this, message, title,
                JOptionPane.ERROR_MESSAGE);
    }

    private void updateSelectedCellLabel() {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();
        if (row >= 0 && column >= 0) {
            cellLabel.setText(
                    "Cell: " + columnName(column + 1) + (row + 1));
        }
    }

    private void refreshGrid() {
        refreshing = true;
        try {
            for (int row = 0; row < ROWS; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    String reference = columnName(column + 1) + (row + 1);
                    Cell cell = spreadsheet.getCell(reference);
                    Object display = "";

                    if (cell != null) {
                        display = cell.getError() != null
                                ? cell.getError()
                                : cell.getDisplayValue();
                    }

                    tableModel.setValueAt(display, row, column);
                }
            }
        } finally {
            refreshing = false;
        }
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

    public static void launch(Spreadsheet spreadsheet) {
        SwingUtilities.invokeLater(() -> {
            SpreadsheetUI ui = new SpreadsheetUI(spreadsheet);
            ui.setVisible(true);
        });
    }
}
