package TASKD;

class inclass01b_bacsain_earllawrence {

    public static void extractLogParts(String log) {
        //these are the anchors
        int openBracket = log.indexOf('[');
        int closeBracket = log.indexOf(']');
        int firstSpace = log.indexOf(' ', openBracket);
        
        //using the first and second anchor ([ and first space) 
        String date = log.substring(openBracket + 1, firstSpace);

        //use the second anchor and last anchor
        String time = log.substring(firstSpace + 1, closeBracket);

        //extract the first word after the close bracket
        int levelStart = closeBracket + 2;
        int levelEnd = log.indexOf(':', levelStart);
        String level = log.substring(levelStart, levelEnd);

        System.out.println("Date: " + date);
        System.out.println("Time: " + time);
        System.out.println("Level: " + level);
    }

    public static void main(String[] args) {
        String log = "[2026-01-31 13:45:02] ERROR: Connection failed.";
        extractLogParts(log);
    }
}