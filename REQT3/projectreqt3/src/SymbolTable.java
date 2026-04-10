// REQT5 — Symbol table
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    public enum Category {
        VARIABLE,
        FUNCTION,
        PARAMETER
    }

    static class SymbolEntry {
        private final String name;
        private final String type;
        private final int scopeLevel;
        private final Category category;

        SymbolEntry(String name, String type, int scopeLevel, Category category) {
            this.name = name;
            this.type = type;
            this.scopeLevel = scopeLevel;
            this.category = category;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public int getScopeLevel() { return scopeLevel; }
        public Category getCategory() { return category; }

        @Override
        public String toString() {
            return String.format("%s(type=%s, scope=%d, category=%s)", name, type, scopeLevel, category);
        }
    }

    private final Deque<Map<String, SymbolEntry>> scopes = new ArrayDeque<>();
    private final List<SymbolEntry> allEntries = new ArrayList<>();
    private int currentLevel = 0;

    public SymbolTable() {
        scopes.push(new HashMap<>());
    }

    public void enterScope() {
        currentLevel++;
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() > 1) {
            scopes.pop();
            currentLevel--;
        }
    }

    public boolean insert(String lexeme, String type) {
        return insert(lexeme, type, Category.VARIABLE);
    }

    public boolean insert(String lexeme, String type, Category category) {
        if (lexeme == null || type == null) return false;
        Map<String, SymbolEntry> current = scopes.peek();
        if (current.containsKey(lexeme)) return false;
        SymbolEntry entry = new SymbolEntry(lexeme, type, currentLevel, category);
        current.put(lexeme, entry);
        allEntries.add(entry);
        return true;
    }

    public SymbolEntry lookup(String lexeme) {
        if (lexeme == null) return null;
        Iterator<Map<String, SymbolEntry>> it = scopes.iterator();
        while (it.hasNext()) {
            SymbolEntry entry = it.next().get(lexeme);
            if (entry != null) return entry;
        }
        return null;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void printTable() {
        System.out.printf("%-20s %-20s %-15s %s%n", "Name", "Type", "Scope Level", "Category");
        for (SymbolEntry e : allEntries) {
            System.out.printf("%-20s %-20s %-15d %s%n",
                e.getName(), e.getType(), e.getScopeLevel(), e.getCategory());
        }
    }
}
