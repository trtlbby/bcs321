import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean parse() {
        block();
        if (current().getType() != TokenType.EOF) {
            error("unexpected token '" + current().getLexeme() + "'");
        }
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void printResult() {
        if (errors.isEmpty()) {
            System.out.println("Syntax analysis: OK");
        } else {
            System.out.println("Syntax analysis: FAILED");
            for (String e : errors) {
                System.out.println("  " + e);
            }
        }
    }

    // --- block and statements ---

    private void block() {
        while (!isBlockEnd() && current().getType() != TokenType.EOF) {
            statement();
        }
    }

    private boolean isBlockEnd() {
        if (current().getType() != TokenType.KEYWORD) return false;
        String kw = current().getLexeme();
        return "end".equals(kw) || "else".equals(kw) || "elseif".equals(kw) || "until".equals(kw);
    }

    private void statement() {
        Token t = current();

        // skip semicolons
        if (t.getType() == TokenType.DELIMITER && ";".equals(t.getLexeme())) {
            advance();
            return;
        }

        if (t.getType() == TokenType.KEYWORD) {
            switch (t.getLexeme()) {
                case "local":    localDecl(); return;
                case "if":       ifStmt(); return;
                case "for":      forStmt(); return;
                case "while":    whileStmt(); return;
                case "repeat":   repeatStmt(); return;
                case "function": funcDecl(); return;
                case "return":   returnStmt(); return;
            }
        }

        if (t.getType() == TokenType.ERROR) {
            error("invalid token '" + t.getLexeme() + "'");
            advance();
            return;
        }

        // assignment or function call — both start with a prefix expression
        if (t.getType() == TokenType.IDENTIFIER) {
            exprStatement();
            return;
        }

        error("unexpected token '" + t.getLexeme() + "'");
        advance();
    }

    // parse IDENTIFIER suffixes then decide: assignment if '=' follows, else must be a call
    private void exprStatement() {
        int startPos = pos;
        boolean hadCall = prefixExpr();

        if (check(TokenType.OPERATOR, "=")) {
            advance(); // consume '='
            expression();
        } else if (!hadCall) {
            error("expected assignment or function call");
        }
    }

    // returns true if the expression ended with a function-call suffix
    private boolean prefixExpr() {
        expect(TokenType.IDENTIFIER, "identifier");
        boolean lastWasCall = false;
        while (true) {
            if (check(TokenType.DELIMITER, ".")) {
                advance();
                expect(TokenType.IDENTIFIER, "field name");
                lastWasCall = false;
            } else if (check(TokenType.DELIMITER, "[")) {
                advance();
                expression();
                expect(TokenType.DELIMITER, "]", "']'");
                lastWasCall = false;
            } else if (check(TokenType.DELIMITER, ":")) {
                advance();
                expect(TokenType.IDENTIFIER, "method name");
                expect(TokenType.DELIMITER, "(", "'('");
                argList();
                expect(TokenType.DELIMITER, ")", "')'");
                lastWasCall = true;
            } else if (check(TokenType.DELIMITER, "(")) {
                advance();
                argList();
                expect(TokenType.DELIMITER, ")", "')'");
                lastWasCall = true;
            } else {
                break;
            }
        }
        return lastWasCall;
    }

    private void argList() {
        if (check(TokenType.DELIMITER, ")")) return;
        expression();
        while (check(TokenType.DELIMITER, ",")) {
            advance();
            expression();
        }
    }

    // --- specific statements ---

    private void localDecl() {
        advance(); // consume 'local'
        if (check(TokenType.KEYWORD, "function")) {
            advance(); // consume 'function'
            expect(TokenType.IDENTIFIER, "function name");
            expect(TokenType.DELIMITER, "(", "'('");
            paramList();
            expect(TokenType.DELIMITER, ")", "')'");
            block();
            expectKeyword("end");
            return;
        }
        expect(TokenType.IDENTIFIER, "variable name");
        if (check(TokenType.OPERATOR, "=")) {
            advance();
            expression();
        }
    }

    private void ifStmt() {
        advance(); // consume 'if'
        expression();
        expectKeyword("then");
        block();
        while (check(TokenType.KEYWORD, "elseif")) {
            advance();
            expression();
            expectKeyword("then");
            block();
        }
        if (check(TokenType.KEYWORD, "else")) {
            advance();
            block();
        }
        expectKeyword("end");
    }

    private void forStmt() {
        advance(); // consume 'for'
        expect(TokenType.IDENTIFIER, "variable name");
        expect(TokenType.OPERATOR, "=", "'='");
        expression();
        expect(TokenType.DELIMITER, ",", "','");
        expression();
        if (check(TokenType.DELIMITER, ",")) {
            advance();
            expression();
        }
        expectKeyword("do");
        block();
        expectKeyword("end");
    }

    private void whileStmt() {
        advance(); // consume 'while'
        expression();
        expectKeyword("do");
        block();
        expectKeyword("end");
    }

    private void repeatStmt() {
        advance(); // consume 'repeat'
        block();
        expectKeyword("until");
        expression();
    }

    private void funcDecl() {
        advance(); // consume 'function'
        expect(TokenType.IDENTIFIER, "function name");
        expect(TokenType.DELIMITER, "(", "'('");
        paramList();
        expect(TokenType.DELIMITER, ")", "')'");
        block();
        expectKeyword("end");
    }

    private void paramList() {
        if (check(TokenType.DELIMITER, ")")) return;
        expect(TokenType.IDENTIFIER, "parameter name");
        while (check(TokenType.DELIMITER, ",")) {
            advance();
            expect(TokenType.IDENTIFIER, "parameter name");
        }
    }

    private void returnStmt() {
        advance(); // consume 'return'
        // return may have no value at end of block
        if (!isBlockEnd() && current().getType() != TokenType.EOF
                && !(current().getType() == TokenType.DELIMITER && ";".equals(current().getLexeme()))) {
            expression();
        }
    }

    // --- expressions (precedence climbing) ---

    private void expression() {
        orExpr();
    }

    private void orExpr() {
        andExpr();
        while (check(TokenType.LOGICAL_OP, "or")) {
            advance();
            andExpr();
        }
    }

    private void andExpr() {
        compExpr();
        while (check(TokenType.LOGICAL_OP, "and")) {
            advance();
            compExpr();
        }
    }

    private void compExpr() {
        concatExpr();
        while (isComparison()) {
            advance();
            concatExpr();
        }
    }

    private boolean isComparison() {
        if (current().getType() != TokenType.OPERATOR) return false;
        String op = current().getLexeme();
        return "<".equals(op) || ">".equals(op) || "<=".equals(op)
                || ">=".equals(op) || "==".equals(op) || "~=".equals(op);
    }

    private void concatExpr() {
        addExpr();
        // '..' is right-associative but for validation we just loop
        while (check(TokenType.OPERATOR, "..")) {
            advance();
            addExpr();
        }
    }

    private void addExpr() {
        mulExpr();
        while (isAddOp()) {
            advance();
            mulExpr();
        }
    }

    private boolean isAddOp() {
        if (current().getType() != TokenType.OPERATOR) return false;
        String op = current().getLexeme();
        return "+".equals(op) || "-".equals(op);
    }

    private void mulExpr() {
        unaryExpr();
        while (isMulOp()) {
            advance();
            unaryExpr();
        }
    }

    private boolean isMulOp() {
        if (current().getType() != TokenType.OPERATOR) return false;
        String op = current().getLexeme();
        return "*".equals(op) || "/".equals(op) || "%".equals(op) || "^".equals(op);
    }

    private void unaryExpr() {
        if (check(TokenType.LOGICAL_OP, "not")) {
            advance();
            unaryExpr();
            return;
        }
        if (check(TokenType.OPERATOR, "-")) {
            advance();
            unaryExpr();
            return;
        }
        primary();
    }

    private void primary() {
        Token t = current();

        switch (t.getType()) {
            case INTEGER_LITERAL:
            case FLOAT_LITERAL:
            case HEX_LITERAL:
            case STRING_LITERAL:
            case BOOLEAN_LITERAL:
            case NIL:
                advance();
                return;
            case IDENTIFIER:
                prefixExpr();
                return;
            case DELIMITER:
                if ("(".equals(t.getLexeme())) {
                    advance();
                    expression();
                    expect(TokenType.DELIMITER, ")", "')'");
                    return;
                }
                if ("{".equals(t.getLexeme())) {
                    tableConstructor();
                    return;
                }
                break;
            default:
                break;
        }
        error("expected expression, got '" + t.getLexeme() + "'");
        advance();
    }

    private void tableConstructor() {
        advance(); // consume '{'
        if (!check(TokenType.DELIMITER, "}")) {
            expression();
            while (check(TokenType.DELIMITER, ",") || check(TokenType.DELIMITER, ";")) {
                advance();
                if (check(TokenType.DELIMITER, "}")) break;
                expression();
            }
        }
        expect(TokenType.DELIMITER, "}", "'}'");
    }

    // --- helpers ---

    private Token current() {
        if (pos >= tokens.size()) return tokens.get(tokens.size() - 1); // EOF
        return tokens.get(pos);
    }

    private void advance() {
        if (pos < tokens.size() - 1) pos++;
    }

    private boolean check(TokenType type, String lexeme) {
        Token t = current();
        return t.getType() == type && lexeme.equals(t.getLexeme());
    }

    private void expect(TokenType type, String description) {
        if (current().getType() == type) {
            advance();
        } else {
            error("expected " + description + ", got '" + current().getLexeme() + "'");
        }
    }

    private void expect(TokenType type, String lexeme, String description) {
        if (check(type, lexeme)) {
            advance();
        } else {
            error("expected " + description + ", got '" + current().getLexeme() + "'");
        }
    }

    private void expectKeyword(String kw) {
        if (check(TokenType.KEYWORD, kw)) {
            advance();
        } else {
            error("expected '" + kw + "', got '" + current().getLexeme() + "'");
        }
    }

    private void error(String message) {
        errors.add("Line " + current().getLine() + ": " + message);
        // panic-mode recovery: skip to next statement-starting token
        recover();
    }

    private void recover() {
        while (current().getType() != TokenType.EOF) {
            Token t = current();
            // stop at statement-starting keywords
            if (t.getType() == TokenType.KEYWORD) {
                String kw = t.getLexeme();
                if ("local".equals(kw) || "if".equals(kw) || "for".equals(kw) || "while".equals(kw)
                        || "repeat".equals(kw) || "function".equals(kw) || "return".equals(kw)
                        || "end".equals(kw) || "else".equals(kw) || "elseif".equals(kw) || "until".equals(kw)) {
                    return;
                }
            }
            // stop at next identifier that starts a line (heuristic: different line)
            if (t.getType() == TokenType.IDENTIFIER && pos > 0 && tokens.get(pos - 1).getLine() < t.getLine()) {
                return;
            }
            advance();
        }
    }
}
