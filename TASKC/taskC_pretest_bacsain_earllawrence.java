package BCS321.TASKC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class taskC_pretest_bacsain_earllawrence {
    // Reserved keywords for Task 4
    private static final String[] RESERVED_KEYWORDS = {
        "int", "float", "if", "while", "double", "for", "break"
    };

    public static void main(String[] args) {
        System.out.println("TASK 1");       
        fileHandler("TASKC/input.txt");
        System.out.println("=================================================");
        System.out.println("TASK 2");
        stringManipulator("int count = 0;");
        System.out.println("=================================================");
        System.out.println("TASK 3");
        List<String> words = stringManipulator("int count = 0;");
        classifyAndPrint(words);
        System.out.println("=================================================");
        System.out.println("TASK 4");
        processFileWithErrorLogging("TASKC/input.txt");
    }

    // Method to classify and print each word
    private static void classifyAndPrint(List<String> words) {
        for (String word : words) {
            System.out.println(word + ": " + classify(word));
        }
    }

    //TASK 1: FILE HANDLING
    private static void fileHandler(String path){
        try {
            File f = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(f));
            ArrayList<String> lines = new ArrayList<String>();

            String line;

            while((line = br.readLine()) != null) {
                System.out.println("Not yet stored line: " + line);
                //add to arraylist
                lines.add(line);
            }
            br.close();

            //check if actually stored in an arraylist
            System.out.println("===============================");
            System.out.println("The stored lines");
            for(String string: lines){
                System.out.println(string);
            }

        } catch (Exception e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }

    /*TASK 2: String Manipulation and Iteration
    Once a line is read, we must break it down.
    1. Take a string like int count = 0;.
    2. Use a Loop (Iterative Statement) to traverse the string character by character.
    3. Conditional Logic: If the character is a space, ignore it. If it is a semicolon, print
    "END_OF_STATEMENT".
    4. Use substring() or StringBuilder to collect characters into words and print them.*/

    private static List<String> stringManipulator(String string) {
        int index = 0;
        int len = string.length();
        List<String> words = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (c == ' ' || c == ';') {
                if (index < i) {
                    String word = string.substring(index, i);
                    words.add(word);
                    System.out.println(word);
                }
                if (c == ';') {
                    System.out.println("END_OF_STATEMENT");
                }
                index = i + 1;
            }
        }
        // Handle last word if no trailing space or semicolon
        if (index < len) {
            String word = string.substring(index, len);
            words.add(word);
            System.out.println(word);
        }
        return words;
    }
    
   
    //TASK 3: Conditional Statements
    /* Now that you have the "words," you must categorize them.
    1. Create a method public String classify(String text).
    2. Use Conditional (if-else/switch) statements to return:
    ○ "NUMBER" if the word is numeric (e.g., 123).
    ○ "IDENTIFIER" if it starts with a letter.
    ○ "OPERATOR" if it is +, -, *, or /.
    3. Iterate through your array from Task 1 and classify every word found.
    My notes: so getting the words from task 1, then passing it to task 2, the task 3
    will categorize it?
    */

    private static String classify(String text){
        if (text.equals("+") || text.equals("-") || text.equals("*") || text.equals("/")) {
            return "OPERATOR";
        } else if (text.matches("\\d+")) {
            return "NUMBER";
        } else if (text.matches("[a-zA-Z]\\w*")) {
            return "IDENTIFIER";
        } else {
            return "UNKNOWN";
        }
    }

    // Task 4: Error Logging (Arrays & Logic)
    // 1. Define an Array of "Reserved Keywords"
    // ("int", "float", "if", "while", “double”, “for”, “break”).
    // 2. As you process words, check if they exist in the keyword array.
    // 3. If a word is not a keyword, not a number, and doesn't follow identifier rules (e.g., starts
    // with a digit like 1var), add it to a separate "Error List" array.
    // 4. Print the final list of errors found in the input.txt.
    
    private static void processFileWithErrorLogging(String path) {
        List<String> errorList = new ArrayList<>();
        try {
            File f = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                List<String> words = stringManipulator(line);
                for (String word : words) {
                    if (isReservedKeyword(word)) {
                        continue;
                    }
                    if (classify(word).equals("NUMBER") || classify(word).equals("OPERATOR") || classify(word).equals("IDENTIFIER")) {
                        continue;
                    }
                    errorList.add(word);
                }
            }
            br.close();
        } catch (Exception e) {
            System.err.println("File not found: " + e.getMessage());
        }
        System.out.println("Errors found in input.txt:");
        if (errorList.isEmpty()) {
            System.out.println("No errors found.");
        } else {
            for (String error : errorList) {
                System.out.println(error);
            }
        }
    }

    private static boolean isReservedKeyword(String word) {
        for (String keyword : RESERVED_KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }
}