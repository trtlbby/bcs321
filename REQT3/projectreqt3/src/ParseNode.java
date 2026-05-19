import java.util.ArrayList;
import java.util.List;

/**
 * A node in the parse tree produced by the Parser.
 *
 * Internal nodes (e.g. "program", "if-stmt", "assign") carry a descriptive
 * label and have zero or more children.  Leaf nodes carry the token lexeme
 * and the source line number where the token appears.
 */
public class ParseNode {

    private final String label;
    private final int    line;       // 0 for internal nodes
    private final List<ParseNode> children = new ArrayList<>();

    /** Construct an internal node (grammar construct). */
    public ParseNode(String label) {
        this.label = label;
        this.line  = 0;
    }

    /** Construct a leaf node (token). */
    public ParseNode(String label, int line) {
        this.label = label;
        this.line  = line;
    }

    public String getLabel()            { return label; }
    public int    getLine()             { return line;  }
    public List<ParseNode> getChildren(){ return children; }

    public void addChild(ParseNode child) {
        if (child != null) children.add(child);
    }

    // ── Tree printing ─────────────────────────────────────────────────────────

    /**
     * Print the full tree rooted at {@code node} to stdout using
     * box-drawing characters for a clear hierarchical view.
     *
     * Example output:
     * <pre>
     * program
     * ├── assign  [line 1]
     * │   ├── x
     * │   ├── =
     * │   └── 10
     * └── if-stmt  [line 3]
     *     ├── if
     *     ...
     * </pre>
     */
    public static void printTree(ParseNode node) {
        System.out.println(node.headerText());
        printChildren(node, "");
    }

    private static void printChildren(ParseNode node, String prefix) {
        List<ParseNode> kids = node.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            boolean last = (i == kids.size() - 1);
            ParseNode child = kids.get(i);

            System.out.println(prefix + (last ? "\\-- " : "+-- ") + child.headerText());

            // recurse with extended prefix
            String childPrefix = prefix + (last ? "    " : "|   ");
            printChildren(child, childPrefix);
        }
    }

    /** Label plus optional line annotation for leaf nodes. */
    private String headerText() {
        if (line > 0) {
            return label + "  [line " + line + "]";
        }
        return label;
    }
}
