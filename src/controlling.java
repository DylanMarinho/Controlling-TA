/****************
 * AUTHOR: Shapagat Boilat
 * Modifications by: Dylan Marinho
 ****************/

import utils.*;

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
            System.out.println(" * -find [find]\t\t Description of the set to find ('min', 'max', 'all') (default: " + Params.DEFAULT_FIND + ")");
            System.out.println(" * -witness\t\t Stop as soon as a full timed-opaque strategy is found  (default: " + Params.DEFAULT_WITNESS + ")");
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

        // Find
        String find = clp.getArgumentValue("find");
        if (find == null) {
            find = Params.DEFAULT_FIND;
        } else {
            Set<String> allowedFindValues = new HashSet<>(List.of(new String[]{"min", "max", "all"}));
            if (!allowedFindValues.contains(find)) {
                System.out.println("ERROR: find value '" + find + "' is not allowed");
                return;
            }
        }
        System.out.println(" * [OPTION] Find: " + find);

        // Witness
        boolean witness = clp.getFlag("witness");
        System.out.println(" * [OPTION] Witness mode: " + witness);

        // Set params of the search
        String mode;
        String type;
        if (Objects.equals(find, "all") && !witness) {
            // If find is all
            mode = "all";
            type = "max";
        } else if (Objects.equals(find, "all") && witness) {
            mode = "first";
            type = "max";
        }
        else if (witness) {
            // Find is not all but witness mode
            mode = "first";
            type = find;
        } else {
            // No witness mode + find is min or max
            mode = "size";
            type = find;
        }


        //Actions
        String actions = clp.getArgumentValue("actions");
        boolean actionFlag = !(actions == null); //True is action are specified in a parameter

        /*WORK*/
        // Open file
        File inputTA = new File(file_name);

        // Edit file (add variables, ...)
        File editedTA = ImitatorManip.createEditedTA(inputTA, loc_final, loc_priv);

        // Get model actions
        Set<String> allActions = ImitatorManip.getActions(editedTA);
        System.out.println(" * [ACTIONS] Actions found in the model: " + allActions);

        // Create the subset of TAs (TAs where some actions are disabled)
        Set<String> actionSet;
        if (actionFlag) {
            actionSet = new HashSet<String>(Arrays.asList(actions.split(",")));
        } else {
            actionSet = allActions;
        }
        System.out.println(" * [ACTIONS] Controllable actions: " + actionSet);

        // Create reachability property files
        File privReachProp = ImitatorManip.createReachFile(editedTA, true, loc_final, loc_priv);
        File pubReachProp = ImitatorManip.createReachFile(editedTA, false, loc_final, loc_priv);

        // Deal the search
        LinkedHashMap<Set<String>, String> subsetsToDisable = Functions.searchSubsets(actionSet, editedTA, mode, type, include_unreach, privReachProp, pubReachProp);
        LinkedHashMap<Set<String>, String> subsetsToAllow = Functions.getSubsetsToAllow(allActions, subsetsToDisable);

        // Write answer
        File outputFile = Functions.writeActionSubset(inputTA, subsetsToAllow);

        // Print answer
        System.out.println("----------------------------------------------------------");
        System.out.println(" * [RESULT] Disabling actions: ");
        System.out.println(subsetsToDisable);
        System.out.println("----------------------------------------------------------");
        System.out.println(" * [RESULT] Found strategies: ");
        System.out.println(subsetsToAllow);
        System.out.println("----------------------------------------------------------");
        System.out.println("Subsets of actions to keep that make the system fully opaque are written in " + outputFile.getPath());
    }
}
