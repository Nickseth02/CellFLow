
package worklet1.parser;

public class ParserDemo {

    public static void main(String[] args) {
        FormulaParser parser = new FormulaParser();

        String[] formulas = {
            "2+3",
            "10-4",
            "3*5",
            "20/4",
            "2+3*4",
            "(2+3)*4"
        };

        System.out.println("===== CellFlow Parser Demo =====");

        for (String formula : formulas) {
            try {
                Object result = parser.parse(formula);

                System.out.println("Input Formula : " + formula);
                System.out.println("Parsed Output : " + result);
                System.out.println("-------------------------------");

            } catch (FormulaParseException e) {
                System.out.println("Input Formula : " + formula);
                System.out.println("Parse Error   : " + e.getMessage());
                System.out.println("-------------------------------");
            }
        }
    }
}
