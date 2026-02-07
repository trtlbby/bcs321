package TASKD;

import java.util.Scanner;

public class inclass01a_bacsain_earllawrence {
    public static boolean isValidIdentifier(String input){
        int min = 3;
        int max = 15;
        
        //rules (1): between 3-15 characters long
        int len = input.length(); //get the len of the input.
        if (len < min && len > max) {
            return false;
        }
        //rules (2): first character must be a letter or an underscore
        if (!(input.charAt(0) == '_' || Character.isLetter(input.charAt(0)))) {
            return false;
        }
        // rules (3): remaining characters can only be a digit, letters, or underscores
        input.toCharArray();
        for(int i = 1; i< len; i++){
            if(!(Character.isLetterOrDigit(input.charAt(i)))){
                return false;
            }
        }
        return true;
    }
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a string (3-15) char length only: ");
        String input =  sc.nextLine();
        if(isValidIdentifier(input)) {
            System.out.printf("The string: %s is valid", input);

       }else {System.out.printf("The string %s is not valid", input);}
       sc.close(); 
    }
    
}
