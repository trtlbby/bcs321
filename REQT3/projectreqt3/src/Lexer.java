// REQT3 — Lexical Analysis driver
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Lexer {
    public static void main(String[] args) throws Exception {
        process(args);
    }

    public static void process(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.out.println("Usage: java App <input.lua>");
            return;
        }

        Path path = Path.of(args[0]);
        if (!Files.exists(path)) {
            System.out.println("File not found: " + path.toAbsolutePath());
            return;
        }

        String source = Files.readString(path);
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        System.out.printf("%-20s %-20s %s%n", "Lexeme", "Token Type", "Line No.");
        for (Token token : tokens) {
            System.out.println(token.formatRow());
        }
    }
}
