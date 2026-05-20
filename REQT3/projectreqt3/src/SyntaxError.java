public class SyntaxError {

    public enum Category {
        EXPECTED,
        INVALID
    }

    private final Category category;
    private final int line;
    private final String expected;
    private final String foundLexeme;
    private final TokenType foundType;

    private SyntaxError(Category category, String expected, Token found) {
        this.category = category;
        this.expected = expected;
        this.line = found.getLine();
        this.foundLexeme = found.getLexeme();
        this.foundType = found.getType();
    }

    public static SyntaxError expected(String expected, Token found) {
        return new SyntaxError(Category.EXPECTED, expected, found);
    }

    public static SyntaxError invalid(Token found) {
        return new SyntaxError(Category.INVALID, null, found);
    }

    public Category getCategory() {
        return category;
    }

    public int getLine() {
        return line;
    }

    public String getExpected() {
        return expected;
    }

    public String getFoundLexeme() {
        return foundLexeme;
    }

    public TokenType getFoundType() {
        return foundType;
    }

    public String format() {
        if (category == Category.INVALID) {
            return String.format("Line %d: invalid token '%s' (%s)", line, foundLexeme, foundType);
        }
        return String.format("Line %d: expected %s, found '%s' (%s)",
                line, expected, foundLexeme, foundType);
    }
}