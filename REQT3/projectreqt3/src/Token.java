public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;

    public Token(TokenType type, String lexeme, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public String formatRow() {
        return String.format("%-20s %-20s %d", lexeme, type, line);
    }

    @Override
    public String toString() {
        return String.format("Token(type=%s, lexeme=%s, line=%d)", type, lexeme, line);
    }
}
