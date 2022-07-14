package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class PolyopManip {
    public static File createPolyopFile(File modelFile) {
        String polyop_file = Params.nameOfPolyopFile(modelFile);
        return FilesManip.createFileNamed(polyop_file);
    }

    public static String getConstraint(String path) {
        StringBuilder output = new StringBuilder();
        String pattern = "BEGIN CONSTRAINT((.|\\n)*)";
        try {

            Scanner scanner = new Scanner(new File(path));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (Pattern.matches(pattern, line)) {
                    line = scanner.nextLine();
                    while (!Pattern.matches("END CONSTRAINT((.|\\n)*)", line)) {
                        output.append(line);
                        line = scanner.nextLine();
                    }
                    return output.toString();
                }
            }
            scanner.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}
