/****************
 * AUTHOR: Shapagat Boilat
 * Modifications by: Dylan Marinho
 ****************/

import utils.FilesManip;
import utils.Functions;
import utils.ImitatorManip;
import utils.Params;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/*
    ASSUMPTIONS:
    • File should contain a single automaton.
    • File should not have comments containing following keyword: clock,
        parameter, bool, continuous, discrete, automaton, sync, do, t_abs, p_abs, visited_qpriv, goto, qf, qpriv.
    • The absolute path of current directory should not contain a space.
    • The private location should be named qpriv.
    • The final location should be named qf
*/


public class controlling {

    static String file_name = "";
    static boolean include_unreach;

    public static void main(String[] args) {

        // Check program options
        if (args.length == 1) { // case where only one argument is set: use default value for unreach
            file_name = args[0];
            include_unreach = Params.default_unreach_option;
        } else if (args.length == 2 && ((args[1].equals("include-unreach") || args[1].equals("exclude-unreach")))) {
            file_name = args[0];
            include_unreach = args[1].equals("include-unreach");
        } else {
            throw new Error("Two program arguments are needed! Syntax: controlling [input_file.imi] ?[include-unreach | exclude-unreach]");
        }

        // Open file
        File inputTA = new File(file_name);

        // Edit file (add variables, ...)
        File editedTA = ImitatorManip.createEditedTA(inputTA);

        // Create the subset of TAs (TAs where some actiosn are disabled)
        ArrayList<File> subsetTAs = Functions.createSubsetTAs(editedTA);

        // Create reachability property files
        File privReachProp = ImitatorManip.createReachFile(editedTA, true);
        File pubReachProp = ImitatorManip.createReachFile(editedTA, false);

        // Run reachability on each sub-TA
        LinkedHashMap<File, File> privImitatorResults = Functions.getImitatorResultsForModels(subsetTAs, privReachProp, "privReach_");
        LinkedHashMap<File, File> pubImitatorResults = Functions.getImitatorResultsForModels(subsetTAs, pubReachProp, "pubReach_");

        // Run polyop and check opacity
        LinkedHashMap<File, File> PolyopResults = Functions.getPolyopResultsForModels(privImitatorResults, pubImitatorResults, include_unreach);
        Set<Set<String>> subsetsToAllow = Functions.getOpaqueSubsets(inputTA, PolyopResults);

        // Print answer
        System.out.println("Following subsets of actions make the system fully opaque:");
        for (Set<String> subset : subsetsToAllow) {

            StringJoiner joiner = new StringJoiner(", ");
            for (String action : subset) {
                joiner.add(action);
            }
            System.out.printf("{%s}%n", joiner);
        }
    }
}
