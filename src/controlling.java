/****************
 * AUTHOR: Shapagat Boilat
 * Modifications by: Dylan Marinho
 ****************/

import utils.CommandLineParser;
import utils.Functions;
import utils.ImitatorManip;
import utils.Keyword;

import java.io.File;
import java.util.*;

/*
    ASSUMPTIONS:
    • File should contain a single automaton.
*/


public class controlling {

    static String file_name = "";
    static boolean include_unreach;

    public static void main(String[] args) {

        /*OPTIONS*/
        CommandLineParser clp = new CommandLineParser(args);

        // HELP
        if (clp.getFlag("h")) {
            System.out.println();
            System.out.println("** Options: **");
            System.out.println("- Required:");
            System.out.println(" * -file [path]\t\t Path to the imi file [REQUIRED]");
            System.out.println("");
            System.out.println("- Optional:");
            System.out.println(" * -actions [actions]\t Controllable actions, separated with a comma (is not set, use all actions of the first automaton)");
            System.out.println(" * -exclude-unreach\t Exclude unreachable in opacity (otherwise, include them)");
            System.out.println(" * -lf [name]\t\t Name of the final location (default: " + Keyword.DEFAULT_LOC_FINAL.toString() + ")");
            System.out.println(" * -lpriv [name]\t Name of the private location (default: " + Keyword.DEFAULT_LOC_PRIV.toString() + ")");
            return;
        }

        // -file option is the .IMI file path
        file_name = clp.getArgumentValue("file");
        if (file_name == null) {
            System.out.println("ERROR: file name is not specified");
            return;
        }

        // if the flag -exclude-unreach is set, exclude. Otherwise, include
        include_unreach = !(clp.getFlag("exclude-unreach"));
        System.out.println(" * [OPTION] Unreachable are included: " + include_unreach);

        //Loc priv, loc final
        String loc_priv = clp.getArgumentValue("lpriv");
        if (loc_priv == null) {
            loc_priv = Keyword.DEFAULT_LOC_PRIV.toString();
        }
        String loc_final = clp.getArgumentValue("lf");
        if (loc_final == null) {
            loc_final = Keyword.DEFAULT_LOC_FINAL.toString();
        }

        //Actions
        String actions = clp.getArgumentValue("actions");
        boolean actionFlag = !(actions == null); //True is action are specified in a parameter

        /*WORK*/
        // Open file
        File inputTA = new File(file_name);

        // Edit file (add variables, ...)
        File editedTA = ImitatorManip.createEditedTA(inputTA, loc_final, loc_priv);

        // Create the subset of TAs (TAs where some actions are disabled)
        Set<String> actionSet;
        if (actionFlag) {
            actionSet = new HashSet<String>(Arrays.asList(actions.split(",")));
        } else {
            actionSet = ImitatorManip.getActions(editedTA);
        }
        ArrayList<File> subsetTAs = Functions.createSubsetTAs(editedTA, actionSet);

        // Create reachability property files
        File privReachProp = ImitatorManip.createReachFile(editedTA, true, loc_final, loc_priv);
        File pubReachProp = ImitatorManip.createReachFile(editedTA, false, loc_final, loc_priv);

        // Run reachability on each sub-TA
        LinkedHashMap<File, File> privImitatorResults = Functions.getImitatorResultsForModels(subsetTAs, privReachProp, "privReach_");
        LinkedHashMap<File, File> pubImitatorResults = Functions.getImitatorResultsForModels(subsetTAs, pubReachProp, "pubReach_");

        // Run polyop and check opacity
        LinkedHashMap<File, File> PolyopResults = Functions.getPolyopResultsForModels(privImitatorResults, pubImitatorResults, include_unreach);
        Set<Set<String>> subsetsToAllow = Functions.getOpaqueSubsets(inputTA, PolyopResults);

        // Write answer
        File outputFile = Functions.writeActionSubset(inputTA, subsetsToAllow);

        // Print answer
        System.out.println("----------------------------------------------------------");
        System.out.println("Subsets of actions make the system fully opaque are written in " + outputFile.getPath());
    }
}
