import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final List<Token> tokens;
    private int pos = 0;
    private int lastLine = 1;
    private boolean lastPrefixWasCall = false;

    private final ErrorHandler errorHandler = new ErrorHandler();
    private final List<String> lineReport = new ArrayList<>();
    private ParseNode root = null;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public boolean parse() {
        root = new ParseNode("program");
        root.addChild(block());
        Token eof = current();
        if (eof.getType() == TokenType.EOF) {
            root.addChild(new ParseNode("EOF", eof.getLine()));
        } else {
            errorExpected("EOF");
        }
        return !errorHandler.hasErrors();
    }

    public ParseNode getTree() { return root; }

    public void printTree() {
        if (root != null) {
            ParseNode.printTree(root);
        } else {
            System.out.println("(no tree — call parse() first)");
        }
    }

    public void printLineReport() {
        System.out.println("Line-by-line report (" + lineReport.size() + " statement(s)):");
        if (lineReport.isEmpty()) {
            System.out.println("  (no statements found)");
        } else {
            for (String entry : lineReport) {
                System.out.println("  " + entry);
            }
        }
    }

    public List<String> getErrors() { return errorHandler.getMessages(); }

    public List<SyntaxError> getSyntaxErrors() { return errorHandler.getSyntaxErrors(); }

    public void printResult() {
        if (!errorHandler.hasErrors()) {
            System.out.println("Syntax analysis: OK");
        } else {
            System.out.println("Syntax analysis: FAILED");
            for (String error : errorHandler.getMessages()) {
                System.out.println("  " + error);
            }
        }
    }

    // ── Block and statements ──────────────────────────────────────────────────

    private ParseNode block() {
        ParseNode node = new ParseNode("block");
        while (!isBlockEnd() && current().getType() != TokenType.EOF) {
            int startLine  = current().getLine();
            int errsBefore = errorHandler.size();
            ParseNode stmt = statement();
            int endLine    = lastLine;
            boolean ok     = errorHandler.size() == errsBefore;
            node.addChild(stmt);
            String range = (startLine == endLine)
                    ? "Line  " + startLine
                    : "Lines " + startLine + "-" + endLine;
            lineReport.add(String.format("%-14s %-20s -> %s",
                    range + ":", stmt.getLabel(), ok ? "OK" : "SYNTAX ERROR"));
        }
        return node;
    }

    private boolean isBlockEnd() {
        if (current().getType() != TokenType.KEYWORD) return false;
        String kw = current().getLexeme();
        return "end".equals(kw) || "else".equals(kw)
                || "elseif".equals(kw) || "until".equals(kw);
    }

    private ParseNode statement() {
        Token t = current();

        if (t.getType() == TokenType.DELIMITER && ";".equals(t.getLexeme())) {
            return advanceLeaf();
        }

        if (t.getType() == TokenType.KEYWORD) {
            switch (t.getLexeme()) {
                case "local":    return localDecl();
                case "if":       return ifStmt();
                case "for":      return forStmt();
                case "while":    return whileStmt();
                case "repeat":   return repeatStmt();
                case "function": return funcDecl();
                case "return":   return returnStmt();
            }
        }

        if (t.getType() == TokenType.ERROR) {
            errorInvalid();
            return advanceLeaf();
        }

        if (t.getType() == TokenType.IDENTIFIER) {
            return exprStatement();
        }

        errorExpected("statement");
        return advanceLeaf();
    }

    // assignment or function call — both start with a prefix expression
    private ParseNode exprStatement() {
        ParseNode prefNode = prefixExpr();
        boolean wasCall = lastPrefixWasCall;

        if (check(TokenType.OPERATOR, "=")) {
            ParseNode assign = new ParseNode("assign");
            assign.addChild(prefNode);
            assign.addChild(advanceLeaf()); // '='
            assign.addChild(expression());
            return assign;
        }
        if (wasCall) {
            ParseNode call = new ParseNode("call");
            call.addChild(prefNode);
            return call;
        }
        errorExpected("assignment or function call");
        ParseNode errNode = new ParseNode("error-stmt");
        errNode.addChild(prefNode);
        return errNode;
    }

    // parse identifier followed by optional suffixes (., [], :method(), ())
    private ParseNode prefixExpr() {
        ParseNode node = new ParseNode("prefix");
        node.addChild(expectLeaf(TokenType.IDENTIFIER, "identifier"));
        lastPrefixWasCall = false;
        while (true) {
            if (check(TokenType.DELIMITER, ".")) {
                node.addChild(advanceLeaf());                            // '.'
                node.addChild(expectLeaf(TokenType.IDENTIFIER, "field name"));
                lastPrefixWasCall = false;
            } else if (check(TokenType.DELIMITER, "[")) {
                node.addChild(advanceLeaf());                            // '['
                node.addChild(expression());
                node.addChild(expectLeaf(TokenType.DELIMITER, "]", "']'"));
                lastPrefixWasCall = false;
            } else if (check(TokenType.DELIMITER, ":")) {
                node.addChild(advanceLeaf());                            // ':'
                node.addChild(expectLeaf(TokenType.IDENTIFIER, "method name"));
                node.addChild(expectLeaf(TokenType.DELIMITER, "(", "'('"));
                node.addChild(argList());
                node.addChild(expectLeaf(TokenType.DELIMITER, ")", "')'"));
                lastPrefixWasCall = true;
            } else if (check(TokenType.DELIMITER, "(")) {
                node.addChild(advanceLeaf());                            // '('
                node.addChild(argList());
                node.addChild(expectLeaf(TokenType.DELIMITER, ")", "')'"));
                lastPrefixWasCall = true;
            } else {
                break;
            }
        }
        // collapse: no suffixes → return just the identifier leaf
        if (node.getChildren().size() == 1) {
            return node.getChildren().get(0);
        }
        return node;
    }

    private ParseNode argList() {
        ParseNode node = new ParseNode("args");
        if (check(TokenType.DELIMITER, ")")) return node;
        node.addChild(expression());
        while (check(TokenType.DELIMITER, ",")) {
            node.addChild(advanceLeaf()); // ','
            node.addChild(expression());
        }
        return node;
    }

    // ── Specific statements ───────────────────────────────────────────────────

    private ParseNode localDecl() {
        // peek ahead to choose the node label before consuming 'local'
        boolean isFunc = pos + 1 < tokens.size()
                && tokens.get(pos + 1).getType() == TokenType.KEYWORD
                && "function".equals(tokens.get(pos + 1).getLexeme());

        ParseNode node = new ParseNode(isFunc ? "local-func" : "local-decl");
        node.addChild(advanceLeaf()); // 'local'

        if (isFunc) {
            node.addChild(advanceLeaf()); // 'function'
            node.addChild(expectLeaf(TokenType.IDENTIFIER, "function name"));
            node.addChild(expectLeaf(TokenType.DELIMITER, "(", "'('"));
            node.addChild(paramList());
            node.addChild(expectLeaf(TokenType.DELIMITER, ")", "')'"));
            node.addChild(block());
            node.addChild(expectKeywordLeaf("end"));
            return node;
        }
        node.addChild(expectLeaf(TokenType.IDENTIFIER, "variable name"));
        if (check(TokenType.OPERATOR, "=")) {
            node.addChild(advanceLeaf()); // '='
            node.addChild(expression());
        }
        return node;
    }

    private ParseNode ifStmt() {
        ParseNode node = new ParseNode("if-stmt");
        node.addChild(advanceLeaf()); // 'if'
        node.addChild(expression());
        node.addChild(expectKeywordLeaf("then"));
        node.addChild(block());
        while (check(TokenType.KEYWORD, "elseif")) {
            ParseNode branch = new ParseNode("elseif-branch");
            branch.addChild(advanceLeaf()); // 'elseif'
            branch.addChild(expression());
            branch.addChild(expectKeywordLeaf("then"));
            branch.addChild(block());
            node.addChild(branch);
        }
        if (check(TokenType.KEYWORD, "else")) {
            ParseNode branch = new ParseNode("else-branch");
            branch.addChild(advanceLeaf()); // 'else'
            branch.addChild(block());
            node.addChild(branch);
        }
        node.addChild(expectKeywordLeaf("end"));
        return node;
    }

    private ParseNode forStmt() {
        ParseNode node = new ParseNode("for-stmt");
        node.addChild(advanceLeaf()); // 'for'
        node.addChild(expectLeaf(TokenType.IDENTIFIER, "variable name"));
        node.addChild(expectLeaf(TokenType.OPERATOR, "=", "'='"));
        node.addChild(expression());
        node.addChild(expectLeaf(TokenType.DELIMITER, ",", "','"));
        node.addChild(expression());
        if (check(TokenType.DELIMITER, ",")) {
            node.addChild(advanceLeaf()); // optional step ','
            node.addChild(expression());
        }
        node.addChild(expectKeywordLeaf("do"));
        node.addChild(block());
        node.addChild(expectKeywordLeaf("end"));
        return node;
    }

    private ParseNode whileStmt() {
        ParseNode node = new ParseNode("while-stmt");
        node.addChild(advanceLeaf()); // 'while'
        node.addChild(expression());
        node.addChild(expectKeywordLeaf("do"));
        node.addChild(block());
        node.addChild(expectKeywordLeaf("end"));
        return node;
    }

    private ParseNode repeatStmt() {
        ParseNode node = new ParseNode("repeat-stmt");
        node.addChild(advanceLeaf()); // 'repeat'
        node.addChild(block());
        node.addChild(expectKeywordLeaf("until"));
        node.addChild(expression());
        return node;
    }

    private ParseNode funcDecl() {
        ParseNode node = new ParseNode("func-decl");
        node.addChild(advanceLeaf()); // 'function'
        node.addChild(expectLeaf(TokenType.IDENTIFIER, "function name"));
        node.addChild(expectLeaf(TokenType.DELIMITER, "(", "'('"));
        node.addChild(paramList());
        node.addChild(expectLeaf(TokenType.DELIMITER, ")", "')'"));
        node.addChild(block());
        node.addChild(expectKeywordLeaf("end"));
        return node;
    }

    private ParseNode paramList() {
        ParseNode node = new ParseNode("params");
        if (check(TokenType.DELIMITER, ")")) return node;
        node.addChild(expectLeaf(TokenType.IDENTIFIER, "parameter name"));
        while (check(TokenType.DELIMITER, ",")) {
            node.addChild(advanceLeaf()); // ','
            node.addChild(expectLeaf(TokenType.IDENTIFIER, "parameter name"));
        }
        return node;
    }

    private ParseNode returnStmt() {
        ParseNode node = new ParseNode("return-stmt");
        node.addChild(advanceLeaf()); // 'return'
        if (!isBlockEnd() && current().getType() != TokenType.EOF
                && !(current().getType() == TokenType.DELIMITER && ";".equals(current().getLexeme()))) {
            node.addChild(expression());
        }
        return node;
    }

    // ── Expressions (precedence climbing) ────────────────────────────────────

    private ParseNode expression() { return orExpr(); }

    private ParseNode orExpr() {
        ParseNode left = andExpr();
        if (!check(TokenType.LOGICAL_OP, "or")) return left;
        ParseNode node = new ParseNode("or-expr");
        node.addChild(left);
        while (check(TokenType.LOGICAL_OP, "or")) {
            node.addChild(advanceLeaf());
            node.addChild(andExpr());
        }
        return node;
    }

    private ParseNode andExpr() {
        ParseNode left = compExpr();
        if (!check(TokenType.LOGICAL_OP, "and")) return left;
        ParseNode node = new ParseNode("and-expr");
        node.addChild(left);
        while (check(TokenType.LOGICAL_OP, "and")) {
            node.addChild(advanceLeaf());
            node.addChild(compExpr());
        }
        return node;
    }

    private ParseNode compExpr() {
        ParseNode left = concatExpr();
        if (!isComparison()) return left;
        ParseNode node = new ParseNode("comp-expr");
        node.addChild(left);
        while (isComparison()) {
            node.addChild(advanceLeaf());
            node.addChild(concatExpr());
        }
        return node;
    }

    private boolean isComparison() {
        if (current().getType() != TokenType.OPERATOR) return false;
        String op = current().getLexeme();
        return "<".equals(op) || ">".equals(op) || "<=".equals(op)
                || ">=".equals(op) || "==".equals(op) || "~=".equals(op);
    }

    private ParseNode concatExpr() {
        ParseNode left = addExpr();
        if (!check(TokenType.OPERATOR, "..")) return left;
        ParseNode node = new ParseNode("concat-expr");
        node.addChild(left);
        while (check(TokenType.OPERATOR, "..")) {
            node.addChild(advanceLeaf());
            node.addChild(addExpr());
        }
        return node;
    }

    private ParseNode addExpr() {
        ParseNode left = mulExpr();
        if (!isAddOp()) return left;
        ParseNode node = new ParseNode("add-expr");
        node.addChild(left);
        while (isAddOp()) {
            node.addChild(advanceLeaf());
            node.addChild(mulExpr());
        }
        return node;
    }

    private boolean isAddOp() {
        if (current().getType() != TokenType.OPERATOR) return false;
        String op = current().getLexeme();
        return "+".equals(op) || "-".equals(op);
    }

    private ParseNode mulExpr() {
        ParseNode left = unaryExpr();
        if (!isMulOp()) return left;
        ParseNode node = new ParseNode("mul-expr");
        node.addChild(left);
        while (isMulOp()) {
            node.addChild(advanceLeaf());
            node.addChild(unaryExpr());
        }
        return node;
    }

    private boolean isMulOp() {
        if (current().getType() != TokenType.OPERATOR) return false;
        String op = current().getLexeme();
        return "*".equals(op) || "/".equals(op) || "%".equals(op) || "^".equals(op);
    }

    private ParseNode unaryExpr() {
        if (check(TokenType.LOGICAL_OP, "not")) {
            ParseNode node = new ParseNode("unary-expr");
            node.addChild(advanceLeaf());  // 'not'
            node.addChild(unaryExpr());
            return node;
        }
        if (check(TokenType.OPERATOR, "-")) {
            ParseNode node = new ParseNode("unary-expr");
            node.addChild(advanceLeaf());  // '-'
            node.addChild(unaryExpr());
            return node;
        }
        return primary();
    }

    private ParseNode primary() {
        Token t = current();
        switch (t.getType()) {
            case INTEGER_LITERAL:
            case FLOAT_LITERAL:
            case HEX_LITERAL:
            case STRING_LITERAL:
            case BOOLEAN_LITERAL:
            case NIL:
                return advanceLeaf();
            case IDENTIFIER:
                return prefixExpr();
            case DELIMITER:
                if ("(".equals(t.getLexeme())) {
                    ParseNode node = new ParseNode("group");
                    node.addChild(advanceLeaf()); // '('
                    node.addChild(expression());
                    node.addChild(expectLeaf(TokenType.DELIMITER, ")", "')'"));
                    return node;
                }
                if ("{".equals(t.getLexeme())) {
                    return tableConstructor();
                }
                break;
            default:
                break;
        }
        errorExpected("expression");
        return advanceLeaf();
    }

    private ParseNode tableConstructor() {
        ParseNode node = new ParseNode("table");
        node.addChild(advanceLeaf()); // '{'
        if (!check(TokenType.DELIMITER, "}")) {
            node.addChild(expression());
            while (check(TokenType.DELIMITER, ",") || check(TokenType.DELIMITER, ";")) {
                node.addChild(advanceLeaf());
                if (check(TokenType.DELIMITER, "}")) break;
                node.addChild(expression());
            }
        }
        node.addChild(expectLeaf(TokenType.DELIMITER, "}", "'}'"));
        return node;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Token current() {
        if (pos >= tokens.size()) return tokens.get(tokens.size() - 1);
        return tokens.get(pos);
    }

    /** Raw advance used by recover(); does not return a node. */
    private void advance() {
        if (pos < tokens.size() - 1) {
            lastLine = current().getLine();
            pos++;
        }
    }

    /** Consume current token, record its line, and return it as a leaf node. */
    private ParseNode advanceLeaf() {
        Token t = current();
        lastLine = t.getLine();
        if (pos < tokens.size() - 1) pos++;
        return new ParseNode(t.getLexeme(), t.getLine());
    }

    private boolean check(TokenType type, String lexeme) {
        Token t = current();
        return t.getType() == type && lexeme.equals(t.getLexeme());
    }

    private ParseNode expectLeaf(TokenType type, String description) {
        if (current().getType() == type) return advanceLeaf();
        errorExpected(description);
        return new ParseNode("<missing " + description + ">");
    }

    private ParseNode expectLeaf(TokenType type, String lexeme, String description) {
        if (check(type, lexeme)) return advanceLeaf();
        errorExpected(description);
        return new ParseNode("<missing " + description + ">");
    }

    private ParseNode expectKeywordLeaf(String kw) {
        if (check(TokenType.KEYWORD, kw)) return advanceLeaf();
        errorExpected("'" + kw + "'");
        return new ParseNode("<missing '" + kw + "'>");
    }

    private void errorExpected(String expected) {
        errorHandler.addExpected(expected, current());
        recover();
    }

    private void errorInvalid() {
        errorHandler.addInvalid(current());
        recover();
    }

    private void recover() {
        while (current().getType() != TokenType.EOF) {
            Token t = current();
            if (t.getType() == TokenType.KEYWORD) {
                String kw = t.getLexeme();
                if ("local".equals(kw) || "if".equals(kw) || "for".equals(kw) || "while".equals(kw)
                        || "repeat".equals(kw) || "function".equals(kw) || "return".equals(kw)
                        || "end".equals(kw) || "else".equals(kw) || "elseif".equals(kw) || "until".equals(kw)) {
                    return;
                }
            }
            if (t.getType() == TokenType.IDENTIFIER && pos > 0
                    && tokens.get(pos - 1).getLine() < t.getLine()) {
                return;
            }
            advance();
        }
    }
}
