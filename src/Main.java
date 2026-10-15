
package worklet1;

import worklet1.model.SpreadsheetModel;
import worklet3.ui.SpreadsheetUI;

public class Main {

    public static void main(String[] args) {
        SpreadsheetModel spreadsheet = new SpreadsheetModel();

        SpreadsheetUI.launch(spreadsheet);
    }
}
