
package worklet3.ui;

import api.Cell;
import api.Spreadsheet;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Spreadsheet UI for Worklet 3, Member 1.
 * Supports direct cell editing and display of stored values.
 */
public final class SpreadsheetUI extends JFrame {

    private static final int ROWS = 50;
    private static final int COLUMNS = 26;

    private final Spreadsheet spreadsheet;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel cellLabel;
    private final JLabel statusLabel;

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

                String reference =
                        columnName(column + 1) + (row + 1);

                String content = value == null
                        ? ""
                        : value.toString();

                commitCell(reference, content);
            }
        };

        table = new JTable(tableModel);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Match the thin spreadsheet grid from Image 1.
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(new Color(160, 174, 190));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowHeight(24);

        table.setSelectionBackground(new Color(190, 215, 240));
        table.setSelectionForeground(Color.BLACK);

        // Keep the grid visually clean while editing.
        table.putClientProperty(
                "terminateEditOnFocusLost", Boolean.TRUE);

        for (int i = 0; i < COLUMNS; i++) {
            table.getColumnModel()
                    .getColumn(i)
                    .setPreferredWidth(100);
        }

        cellLabel = new JLabel("Cell: A1");
        statusLabel = new JLabel(
                "Ready. Select a cell and type to enter a value.");

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(cellLabel);

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
            if (content.startsWith("=")) {
                statusLabel.setText(
                        "Formula evaluation is not integrated yet. "
                        + "Worklet 2 is required.");
                refreshGrid();
                return;
            }

            spreadsheet.setCell(reference, content);
            refreshGrid();

            statusLabel.setText("Updated " + reference);

        } catch (RuntimeException exception) {
            statusLabel.setText(
                    "Error: " + (exception.getMessage() == null
                            ? "Unable to update cell."
                            : exception.getMessage()));

            refreshGrid();
        }

        updateSelectedCellLabel();
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
                    String reference =
                            columnName(column + 1) + (row + 1);

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
