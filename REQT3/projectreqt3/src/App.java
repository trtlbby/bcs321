import java.util.List;

public class App {
    private static final String SEP = "=".repeat(62);

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java App <mode> <input.lua>");
            System.out.println("Modes: tokens, symbols, parse");
            return;
        }

        String mode = args[0];
        String source = Lexer.readSource(args[1]);
        List<Token> tokens = Lexer.tokenize(source);

        switch (mode) {
            case "tokens":
                Lexer.printTokens(tokens);
                break;
            case "symbols":
                SymbolTable table = Lexer.buildSymbolTable(tokens);
                table.printTable();
                break;
            case "parse":
                Parser parser = new Parser(tokens);
                parser.parse();
                printSection("PARSE TREE");
                parser.printTree();
                printSection("LINE-BY-LINE REPORT");
                parser.printLineReport();
                printSection("SYNTAX ANALYSIS RESULT");
                parser.printResult();
                break;
            default:
                System.out.println("Unknown mode: " + mode);
                System.out.println("Modes: tokens, symbols, parse");
        }
    }

    private static void printSection(String title) {
        System.out.println(SEP);
        System.out.println(title);
        System.out.println(SEP);
    }
}
