package utils;

import java.io.File;
import java.util.StringJoiner;

public class Params {
    static public boolean default_unreach_option = false;

    // Polyop and IMITATOR are supposed to be known in the PATH
    // If not, it is possible to update the path here:
    static public String PathImitator = "imitator";
    static public String PathPolyop = "polyop";

    // Directory to store all outputs
    static public String pathToOutput = "output";

    // Define naming methods
    /** Return the name for the edited TA
     * @param inputFile Imi file in input (file.imi)
     * @return Return name of edited version (file_edited.imi) */
    static public String nameOfEditedTA(File inputFile) {
        String inputName = FilesManip.getNameWithoutExtension(inputFile.getName());
        return String.format("%s_edited.imi", inputName);
    }
    static public String nameOfPrivateReachProperty(File inputFile) {
        String inputName = FilesManip.getNameWithoutExtension(inputFile.getName());
        return String.format("reachPriv_%s.imiprop", inputName);
    }
    static public String nameOfPublicReachProperty(File inputFile) {
        String inputName = FilesManip.getNameWithoutExtension(inputFile.getName());
        return String.format("reachPub_%s.imiprop", inputName);
    }
    static public String nameOfSubTA(File inputFile, StringJoiner joiner) {
        String fileNameWithoutExtension = FilesManip.getNameWithoutExtension(inputFile.getName());
        return String.format("%s_%s.imi", fileNameWithoutExtension, joiner);
    }
}
