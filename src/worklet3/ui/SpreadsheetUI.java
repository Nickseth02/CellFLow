
package worklet3.ui;

import api.Cell;
import api.Spreadsheet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import worklet3.persistence.SheetLoader;
import worklet3.persistence.SheetSerializer;

/**
 * Spreadsheet UI for Worklet 3.
 * Supports editing, copy-paste, and saving/loading cell contents.
 */
public final class SpreadsheetUI extends JFrame {

    private static final int ROWS = 50;
    private static final int COLUMNS = 26;

    private final Spreadsheet spreadsheet;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel cellLabel;
    private final JLabel statusLabel;

    // Stores original contents, including formulas, entered through this UI.
    private final Map<String, String> originalContents =
            new LinkedHashMap<>();

    private boolean refreshing;
    private String copiedContent = "";

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

        JButton saveButton = new JButton("Save");
        JButton loadButton = new JButton("Load");

        saveButton.addActionListener(event -> saveSheet());
        loadButton.addActionListener(event -> loadSheet());

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

        installCopyPaste();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);

        refreshGrid();
        updateSelectedCellLabel();
    }

    private void commitCell(String reference, String content) {
        try {
            spreadsheet.setCell(reference, content);

            if (content.isEmpty()) {
                originalContents.remove(reference);
            } else {
                originalContents.put(reference, content);
            }

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

    private void installCopyPaste() {
        table.getInputMap(JTable.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("ctrl C"), "copy-cell");

        table.getActionMap().put("copy-cell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int row = table.getSelectedRow();
                int column = table.getSelectedColumn();

                if (row < 0 || column < 0) {
                    return;
                }

                String reference =
                        columnName(column + 1) + (row + 1);

                if (originalContents.containsKey(reference)) {
                    copiedContent = originalContents.get(reference);
                } else {
                    copiedContent = "";
                }

                statusLabel.setText("Copied " + reference);
            }
        });

        table.getInputMap(JTable.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("ctrl V"), "paste-cell");

        table.getActionMap().put("paste-cell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int row = table.getSelectedRow();
                int column = table.getSelectedColumn();

                if (row < 0 || column < 0) {
                    return;
                }

                String reference =
                        columnName(column + 1) + (row + 1);

                commitCell(reference, copiedContent);
            }
        });
    }

    private void saveSheet() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Spreadsheet");

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();

        try {
            SheetSerializer serializer = new SheetSerializer();
            serializer.save(
                    new LinkedHashMap<>(originalContents), path);

            statusLabel.setText(
                    "Saved spreadsheet: " + path.getFileName());

        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not save spreadsheet:\n"
                            + exception.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
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
            SheetLoader loader = new SheetLoader();

            // Read and validate the file before changing the sheet.
            Map<String, String> loadedContents = loader.load(path);

            // Clear cells tracked by this UI before applying loaded data.
            for (String reference : originalContents.keySet()) {
                if (!loadedContents.containsKey(reference)) {
                    spreadsheet.setCell(reference, "");
                }
            }

            for (Map.Entry<String, String> entry
                    : loadedContents.entrySet()) {
                spreadsheet.setCell(entry.getKey(), entry.getValue());
            }

            originalContents.clear();
            originalContents.putAll(loadedContents);

            refreshGrid();

            statusLabel.setText(
                    "Loaded spreadsheet: " + path.getFileName());

        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load spreadsheet:\n"
                            + exception.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
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

                        if (display == null) {
                            display = "";
                        }
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
