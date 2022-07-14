/****************
 * AUTHOR: Shapagat Boilat
 * Modifications by: Dylan Marinho
 ****************/

import utils.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            System.out.println(" * -mode [mode]\t\t Set when to stop the search ('first', 'size', 'all') (default: " + Params.DEFAULT_MODE + ")");
            return;
        }

        // -file option is the .IMI file path
        file_name = clp.getArgumentValue("file");
        if (file_name == null) {
            System.out.println("ERROR: file name is not specified");
            return;
        }

        // if the flag -exclude-unreach is set, exclude. Otherwise, include
        include_unreach = !(clp.getFlag("excludeunreach"));
        //NOTE: there is no - between words in getFlag! (cf. getFlag() with replace("-","")
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

        String mode = clp.getArgumentValue("mode");
        if (mode == null) {
            mode = Params.DEFAULT_MODE;
        } else {
            Set<String> allowedModeValues = new HashSet<>(List.of(new String[]{"first", "size", "all"}));
            if (!allowedModeValues.contains(mode)) {
                System.out.println("ERROR: mode value '" + mode + "' is not allowed");
                return;
            }
        }
        System.out.println(" * [OPTION] Mode: " + mode);

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

        // Create reachability property files
        File privReachProp = ImitatorManip.createReachFile(editedTA, true, loc_final, loc_priv);
        File pubReachProp = ImitatorManip.createReachFile(editedTA, false, loc_final, loc_priv);

        // Deal the search
        Set<Set<String>> subsetsToDisable = Functions.searchSubsets(actionSet, editedTA, mode, include_unreach, privReachProp, pubReachProp);
        Set<Set<String>> subsetsToAllow = Functions.getSubsetsToAllow(editedTA, subsetsToDisable);
        System.out.println(subsetsToAllow);

        // Write answer
        File outputFile = Functions.writeActionSubset(inputTA, subsetsToAllow);

        // Print answer
        System.out.println("----------------------------------------------------------");
        System.out.println("Subsets of actions to keep that make the system fully opaque are written in " + outputFile.getPath());
    }
}
