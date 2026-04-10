// REQT3 — Lexical Analysis driver
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Lexer {
    public static void main(String[] args) throws Exception {
        process(args);
    }

    public static List<Token> tokenize(String source) {
        Scanner scanner = new Scanner(source);
        return scanner.scanTokens();
    }

    public static String readSource(String filePath) throws Exception {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new Exception("File not found: " + path.toAbsolutePath());
        }
        return Files.readString(path);
    }

    public static void printTokens(List<Token> tokens) {
        System.out.printf("%-20s %-20s %s%n", "Lexeme", "Token Type", "Line No.");
        for (Token token : tokens) {
            System.out.println(token.formatRow());
        }
    }

    public static void process(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.out.println("Usage: java App <input.lua>");
            return;
        }
        String source = readSource(args[0]);
        List<Token> tokens = tokenize(source);
        printTokens(tokens);
    }

    /**
     * Walk tokens to build a symbol table.
     * Tracks local declarations, global assignments, and function definitions.
     */
    public static SymbolTable buildSymbolTable(List<Token> tokens) {
        SymbolTable table = new SymbolTable();
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);

            // scope tracking: enter on 'then', 'do', or 'function'; exit on 'end'
            if (t.getType() == TokenType.KEYWORD) {
                String kw = t.getLexeme();
                if ("then".equals(kw) || "do".equals(kw)) {
                    table.enterScope();
                } else if ("end".equals(kw)) {
                    table.exitScope();
                } else if ("function".equals(kw) && i + 1 < tokens.size()
                        && tokens.get(i + 1).getType() == TokenType.IDENTIFIER) {
                    table.insert(tokens.get(i + 1).getLexeme(), "function", SymbolTable.Category.FUNCTION);
                    table.enterScope();
                    i++;
                } else if ("local".equals(kw) && i + 1 < tokens.size()
                        && tokens.get(i + 1).getType() == TokenType.IDENTIFIER) {
                    String name = tokens.get(i + 1).getLexeme();
                    String type = inferType(tokens, i + 2);
                    table.insert(name, type);
                    i++;
                }
            }

            // global assignment: IDENTIFIER directly followed by '=' (not '.', '[', or ':' before it)
            if (t.getType() == TokenType.IDENTIFIER && i + 1 < tokens.size()) {
                Token next = tokens.get(i + 1);
                if (next.getType() == TokenType.OPERATOR && "=".equals(next.getLexeme())) {
                    // skip table field access like tbl.key = ... or arr[1] = ...
                    boolean isFieldAccess = i > 0 && tokens.get(i - 1).getType() == TokenType.DELIMITER
                            && (".".equals(tokens.get(i - 1).getLexeme())
                                || ":".equals(tokens.get(i - 1).getLexeme())
                                || "]".equals(tokens.get(i - 1).getLexeme()));
                    if (!isFieldAccess && table.lookup(t.getLexeme()) == null) {
                        String type = inferType(tokens, i + 1);
                        table.insert(t.getLexeme(), type);
                    }
                }
            }
        }
        return table;
    }

    // Infer type from the token after '=' (e.g. local x = 10 → "integer")
    private static String inferType(List<Token> tokens, int pos) {
        // expect '=' at pos, value at pos+1
        if (pos + 1 >= tokens.size()) return "unknown";
        Token eq = tokens.get(pos);
        if (!(eq.getType() == TokenType.OPERATOR && "=".equals(eq.getLexeme()))) return "unknown";

        Token val = tokens.get(pos + 1);
        switch (val.getType()) {
            case INTEGER_LITERAL: return "integer";
            case FLOAT_LITERAL:   return "float";
            case HEX_LITERAL:     return "hex";
            case STRING_LITERAL:  return "string";
            case BOOLEAN_LITERAL: return "boolean";
            case NIL:             return "nil";
            case IDENTIFIER:      return "identifier";
            default:              return "unknown";
        }
    }
}
