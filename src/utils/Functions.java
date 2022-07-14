package utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Functions {


    /**
     * From a set of actions, return all subsets
     *
     * @param actions Set of actions
     * @return Set of all possible combinations of actions
     */
    public static Set<Set<String>> getSubsetsOfActions(Set<String> actions) {
        SetOfActions set = new SetOfActions(actions);
        Set<String> currentSubset;
        Set<Set<String>> subsets = new LinkedHashSet<>();
        while (set.increment()) {
            currentSubset = set.getSet();
            subsets.add(currentSubset);
        }
        return subsets;
    }

    /**
     * Search for subsets of actions to ensure opacity
     *
     * @param actions Set of actions
     * @param mode    Mode of search (first / size / all)
     * @return Set of subsets that ensure full timed opacity
     */
    public static Set<Set<String>> searchSubsets(Set<String> actions, File editedFile, String mode, boolean include_unreach, File privReachProp, File pubReachProp) {
        // Initalize action-subset search
        SetOfActions set = new SetOfActions(actions);
        Set<String> subset;
        Set<Set<String>> result = new HashSet<>();
        boolean found = false;
        int sizeOfFound = actions.size() + 1; // More than all the possible sizes of subsets

        // For all subset
        while (set.increment()) {

            if (found && Objects.equals(mode, "size") && set.getSize() > sizeOfFound) {
                // If mode is size, a result has been found and we are going to the next size
                return result;
            }

            subset = set.getSet();
            File subsetTA = createSubsetTA(editedFile, subset);

            // Make runs and get results
            File privImitatorResult = getImitatorResultsForModel(subsetTA, privReachProp, "privReach_");
            File pubImitatorResult = getImitatorResultsForModel(subsetTA, pubReachProp, "pubReach_");

            // Check if the result has to be considered
            // eg. do not consider unreachable if include_unreachable is set to false
            if (considerSubset(privImitatorResult, pubImitatorResult, include_unreach)) {

                // Run polyop and get result
                File polyopResult = getPolyopResultForModel(subsetTA, privImitatorResult, pubImitatorResult);

                if (isOpaqueSubset(polyopResult)) {
                    result.add(subset);

                    if (Objects.equals(mode, "first")) {
                        return result;
                    }

                    found = true;
                    sizeOfFound = set.getSize();
                }
            }
        }
        return result;
    }

    public static Set<Set<String>> getSubsetsToAllow(File inputTA, Set<Set<String>> subsetsToDisable) {
        Set<Set<String>> subsetsToAllow = new LinkedHashSet<>();
        Set<String> model_actions = ImitatorManip.getActions(inputTA);

        for (Set<String> subset : subsetsToDisable) {
            Set<String> model_copy = new HashSet<>(model_actions);
            model_copy.removeAll(subset);
            subsetsToAllow.add(model_copy);
        }
        return subsetsToAllow;
    }

    private static boolean considerSubset(File privImitatorResult, File pubImitatorResult, boolean include_unreach) {
        if (!include_unreach) {
            String privConstraint = PolyopManip.getConstraint(privImitatorResult.getPath());
            String pubConstraint = PolyopManip.getConstraint(pubImitatorResult.getPath());
            return !privConstraint.equals("False") || !pubConstraint.equals("False");
        }
        return true;
    }

    private static File createSubsetTA(File inputFile, Set<String> subset) {
        StringJoiner joiner = new StringJoiner("_");
        StringJoiner output_line = new StringJoiner(",");
        for (String action : subset) {
            joiner.add(action);
            output_line.add(action);
        }

        String fileName = Params.nameOfSubTA(inputFile, joiner);
        File outputFile = FilesManip.createFileNamed(fileName);

        try {
            Scanner scanner = new Scanner(inputFile);
            boolean disabler_found = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();


                if (disabler_found) {
                    if (line.contains(Keyword.SYNCLABS + ":")) {
                        FilesManip.addLine(outputFile, String.format("%s: %s;", Keyword.SYNCLABS, output_line));

                    } else {
                        FilesManip.addLine(outputFile, line);
                    }
                } else {
                    if (line.contains("disabler")) {
                        disabler_found = true;
                        FilesManip.addLine(outputFile, line);
                    } else {
                        FilesManip.addLine(outputFile, line);
                    }
                }

            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return outputFile;
    }

    private static boolean isOpaqueSubset(File polyopResult) {
        try {
            Scanner scanner = new Scanner(polyopResult);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("yes")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Run a command in terminal
     *
     * @param command Command to run
     */
    private static void runTerminal(String[] command) {
        try {
            System.out.println("* [RUN] " + String.join(" ", command));
            Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static File getImitatorResultsForModel(File model, File propertyFile, String outputPrefix) {
        String propertyPath = propertyFile.getPath();
        String res_name_without_extension = Params.nameOfResImitatorFile(model.getName(), outputPrefix);

        //Run
        String[] cmd = {Params.PathImitator, model.getPath(), propertyPath, "-output-prefix", Params.pathToOutput + "/" + res_name_without_extension};
        runTerminal(cmd);

        String res_name_with_extension = res_name_without_extension + ".res";

        return new File(Params.pathToOutput + "/" + res_name_with_extension);
    }

    private static File getPolyopResultForModel(File inputModel, File privImitatorResult, File pubImitatorResult) {
        String privPath = privImitatorResult.getPath();
        String pubPath = pubImitatorResult.getPath();

        String privConstraint = PolyopManip.getConstraint(privPath);
        String pubConstraint = PolyopManip.getConstraint(pubPath);

        //checking if both are reachable then will create a .polyop file
        File polyOpPath = PolyopManip.createPolyopFile(inputModel);

        String content = String.format("equal (%s,%s)", pubConstraint, privConstraint);
        FilesManip.writeToFile(content, polyOpPath, false);

        runTerminal(new String[]{Params.PathPolyop, polyOpPath.getPath()});
        String resPath = polyOpPath.getPath() + ".res";
        File result = new File(resPath);
        return result;
    }

    public static File writeActionSubset(File modelFile, Set<Set<String>> subsetsToAllow) {
        String outputFileName = Params.nameOfActionSubsetOutput(modelFile);
        File outputFile = FilesManip.createFileNamed(outputFileName);
        for (Set<String> subset : subsetsToAllow) {

            StringJoiner joiner = new StringJoiner(", ");
            for (String action : subset) {
                joiner.add(action);
            }
            FilesManip.addLine(outputFile, "{" + joiner + "}");
        }
        return outputFile;
    }
}
