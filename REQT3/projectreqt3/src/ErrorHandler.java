import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {

    private final List<SyntaxError> syntaxErrors = new ArrayList<>();

    public void addExpected(String expected, Token found) {
        syntaxErrors.add(SyntaxError.expected(expected, found));
    }

    public void addInvalid(Token found) {
        syntaxErrors.add(SyntaxError.invalid(found));
    }

    public boolean hasErrors() {
        return !syntaxErrors.isEmpty();
    }

    public int size() {
        return syntaxErrors.size();
    }

    public List<SyntaxError> getSyntaxErrors() {
        return new ArrayList<>(syntaxErrors);
    }

    public List<String> getMessages() {
        List<String> messages = new ArrayList<>();
        for (SyntaxError syntaxError : syntaxErrors) {
            messages.add(syntaxError.format());
        }
        return messages;
    }
}