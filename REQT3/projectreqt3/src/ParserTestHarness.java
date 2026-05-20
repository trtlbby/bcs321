import java.util.ArrayList;
import java.util.List;

public class ParserTestHarness {

    private static final String SEP = "=".repeat(72);

    public static void main(String[] args) {
        List<TestCase> tests = buildTests();
        int passed = 0;

        System.out.println(SEP);
        System.out.println("Parser regression harness");
        System.out.println(SEP);

        for (int i = 0; i < tests.size(); i++) {
            TestResult result = runCase(tests.get(i));
            if (result.passed) {
                passed++;
            }
            printResult(i + 1, tests.size(), result);
        }

        System.out.println(SEP);
        System.out.printf("Passed %d/%d tests%n", passed, tests.size());

        if (passed != tests.size()) {
            System.exit(1);
        }
    }

    private static List<TestCase> buildTests() {
        List<TestCase> tests = new ArrayList<>();

        tests.add(TestCase.valid(
                "valid local assignment",
                "local x = 10",
                "local-decl"
        ));

        tests.add(TestCase.valid(
                "valid if then end block",
                "if x then x = 1 end",
                "if-stmt"
        ));

        tests.add(TestCase.valid(
                "grouped arithmetic precedence",
                "a = (b + c) * d",
                "mul-expr",
                "group"
        ));

        tests.add(TestCase.invalid(
                "missing end block",
                "if x then x = 1",
                "'end'",
                TokenType.EOF,
                1,
                null
        ));

        tests.add(TestCase.invalid(
                "unexpected token after if",
                "if end",
                "expression",
                TokenType.KEYWORD,
                1,
                "end"
        ));

        return tests;
    }

    private static TestResult runCase(TestCase testCase) {
        List<String> failures = new ArrayList<>();
        Parser parser = new Parser(Lexer.tokenize(testCase.source));
        boolean actualSuccess = parser.parse();
        ParseNode root = parser.getTree();

        if (actualSuccess != testCase.expectedSuccess) {
            failures.add(String.format("expected parse success=%s but got %s",
                    testCase.expectedSuccess, actualSuccess));
        }

        if (root == null) {
            failures.add("parser did not produce a tree root");
        } else if (!"program".equals(root.getLabel())) {
            failures.add("parse tree root label was not 'program'");
        }

        for (String requiredLabel : testCase.requiredLabels) {
            if (root == null || !containsLabel(root, requiredLabel)) {
                failures.add("parse tree did not contain label '" + requiredLabel + "'");
            }
        }

        if (!testCase.expectedSuccess) {
            validateErrorExpectations(testCase, parser.getSyntaxErrors(), failures);
        }

        return new TestResult(testCase.name, failures.isEmpty(), failures);
    }

    private static void validateErrorExpectations(TestCase testCase, List<SyntaxError> errors, List<String> failures) {
        if (errors.isEmpty()) {
            failures.add("expected at least one syntax error but parser reported none");
            return;
        }

        SyntaxError firstError = errors.get(0);

        if (!testCase.expectedToken.equals(firstError.getExpected())) {
            failures.add(String.format("expected first error target '%s' but got '%s'",
                    testCase.expectedToken, firstError.getExpected()));
        }

        if (testCase.expectedFoundType != firstError.getFoundType()) {
            failures.add(String.format("expected first error token type %s but got %s",
                    testCase.expectedFoundType, firstError.getFoundType()));
        }

        if (testCase.expectedLine != firstError.getLine()) {
            failures.add(String.format("expected first error line %d but got %d",
                    testCase.expectedLine, firstError.getLine()));
        }

        if (testCase.expectedFoundLexemeFragment != null
                && !firstError.getFoundLexeme().contains(testCase.expectedFoundLexemeFragment)) {
            failures.add(String.format("expected first error lexeme to contain '%s' but got '%s'",
                    testCase.expectedFoundLexemeFragment, firstError.getFoundLexeme()));
        }
    }

    private static boolean containsLabel(ParseNode node, String label) {
        if (label.equals(node.getLabel())) {
            return true;
        }
        for (ParseNode child : node.getChildren()) {
            if (containsLabel(child, label)) {
                return true;
            }
        }
        return false;
    }

    private static void printResult(int index, int total, TestResult result) {
        System.out.printf("[%d/%d] %s -> %s%n",
                index,
                total,
                result.name,
                result.passed ? "PASS" : "FAIL");

        for (String failure : result.failures) {
            System.out.println("  " + failure);
        }
    }

    private static class TestCase {
        private final String name;
        private final String source;
        private final boolean expectedSuccess;
        private final String[] requiredLabels;
        private final String expectedToken;
        private final TokenType expectedFoundType;
        private final int expectedLine;
        private final String expectedFoundLexemeFragment;

        private TestCase(String name,
                         String source,
                         boolean expectedSuccess,
                         String[] requiredLabels,
                         String expectedToken,
                         TokenType expectedFoundType,
                         int expectedLine,
                         String expectedFoundLexemeFragment) {
            this.name = name;
            this.source = source;
            this.expectedSuccess = expectedSuccess;
            this.requiredLabels = requiredLabels;
            this.expectedToken = expectedToken;
            this.expectedFoundType = expectedFoundType;
            this.expectedLine = expectedLine;
            this.expectedFoundLexemeFragment = expectedFoundLexemeFragment;
        }

        private static TestCase valid(String name, String source, String... requiredLabels) {
            return new TestCase(name, source, true, requiredLabels, null, null, 0, null);
        }

        private static TestCase invalid(String name,
                                        String source,
                                        String expectedToken,
                                        TokenType expectedFoundType,
                                        int expectedLine,
                                        String expectedFoundLexemeFragment) {
            return new TestCase(name, source, false, new String[0], expectedToken,
                    expectedFoundType, expectedLine, expectedFoundLexemeFragment);
        }
    }

    private static class TestResult {
        private final String name;
        private final boolean passed;
        private final List<String> failures;

        private TestResult(String name, boolean passed, List<String> failures) {
            this.name = name;
            this.passed = passed;
            this.failures = failures;
        }
    }
}