import java.util.List;

/**
 * projectreqt7_BACSAIN.java
 *
 * Parser module — secondary phase of the Lua compiler.
 * Consumes the token stream from the Lexer/Scanner and performs
 * syntactic analysis, producing a Parse Tree and per-statement report.
 *
 * Usage:
 *   java projectreqt7_BACSAIN [source.lua]
 *
 * Defaults to projectreqt7_BACSAIN_input.lua when no argument is given.
 */
public class projectreqt7_BACSAIN {

    private static final String SEP   = "=".repeat(62);
    private static final String INPUT = "projectreqt7_BACSAIN_input.lua";

    public static void main(String[] args) throws Exception {
        String filePath = (args.length > 0) ? args[0] : INPUT;

        printHeader(filePath);

        // ── Phase 1: Lexical Analysis ─────────────────────────────────────────
        String source       = Lexer.readSource(filePath);
        List<Token> tokens  = Lexer.tokenize(source);

        section("SECTION 1 -- TOKEN STREAM");
        Lexer.printTokens(tokens);

        // ── Phase 2: Syntactic Analysis ───────────────────────────────────────
        Parser parser = new Parser(tokens);
        parser.parse();

        section("SECTION 2 -- PARSE TREE");
        parser.printTree();

        section("SECTION 3 -- LINE-BY-LINE REPORT");
        parser.printLineReport();

        section("SECTION 4 -- SYNTAX ANALYSIS RESULT");
        parser.printResult();

        System.out.println(SEP);
    }

    private static void printHeader(String filePath) {
        System.out.println(SEP);
        System.out.println("  Lua Compiler  |  Parser Module");
        System.out.println("  projectreqt7_BACSAIN");
        System.out.println("  Source file : " + filePath);
        System.out.println(SEP);
    }

    private static void section(String title) {
        System.out.println("\n" + SEP);
        System.out.println("  " + title);
        System.out.println(SEP);
    }
}
