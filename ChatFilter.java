import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatFilter class
 *
 * Takes in each String to be broadcast and filters it based on specific 'bad' words from a text file
 *
 * @author Christopher Lehman
 *
 * @version 11/13/18
 *
 */

public class ChatFilter {

    private List<String> words;

    public ChatFilter(String badWordsFileName) {
        File file = new File(badWordsFileName);
        words = new ArrayList<>();
        //add all words in file to list for future checking
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }
                words.add(line);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String filter(String msg) {
        if (msg == null) {
            return null;
        }
        String replacement = "";
        for (String s : words) {
            for (int i = 0; i < s.length(); i++) { // gets replacement string of proper length
                replacement += "*";
            }
            msg = msg.replaceAll("(?i)" + s, replacement); //case-insensitive
            replacement = "";
        }
        return msg;
    }
}
