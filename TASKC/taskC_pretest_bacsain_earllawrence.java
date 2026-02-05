package BCS321.TASKC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class taskC_pretest_bacsain_earllawrence {
    public static void main(String[] args) {
        System.out.println("TASK 1");       
        fileHandler("TASKC/input.txt");
        System.out.println("=================================================");
        System.out.println("TASK 2");
        stringManipulator("int count = 0;");
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

    private static void stringManipulator(String string) {
        String str;
        str = string;

        int i, len = str.length();
        for(i = 0; i < len; i++){
            char characters = str.charAt(i);
            char condition = ';';
            char spaceCondition = " ";

            if (str.charAt(i) == condition) {
                System.out.println("END_OF_STATEMENT");
            } else if (str.charAt(i) == spaceCondition){
                continue;
            }else {
                System.out.println("Character at " + i + " is: " + characters);
            }
        }
    }
}