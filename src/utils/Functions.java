package utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Functions {

    /**
     * Search for subsets of actions to ensure opacity
     *
     * @param actions         Set of (controllable) actions
     * @param editedFile      File of the editedTA
     * @param mode            Mode of search (first / size / all)
     * @param type            Type of result (max/min)
     * @param include_unreach Include unreachable results
     * @param privReachProp   File to private property
     * @param pubReachProp    File to public property
     * @return Set of subsets that ensure full timed opacity
     */
    public static LinkedHashMap<Set<String>, String> searchSubsets(Set<String> actions, File editedFile, String mode, String type, boolean include_unreach, File privReachProp, File pubReachProp, String imitator_path, String polyop_path) {
        // Initalize action-subset search
        SetOfActions set = new SetOfActions(actions);
        Set<String> subset;
        LinkedHashMap<Set<String>, String> result = new LinkedHashMap<>();
        boolean found = false;
        int sizeOfFound = actions.size() + 1; // More than all the possible sizes of subsets

        // For all subset
        while (set.increment()) {

            if (found && Objects.equals(mode, "size") && set.getSize() > sizeOfFound) {
                // If mode is size, a result has been found and we are going to the next size
                return result;
            }

            if (Objects.equals(type, "max")) {
                subset = set.getSet();
            } else {
                subset = excludeActions(actions, set.getSet());
            }
            ;
            File subsetTA = createSubsetTA(editedFile, subset);

            // Make runs and get results
            File privImitatorResult = getImitatorResultsForModel(subsetTA, privReachProp, "privReach_", imitator_path);
            File pubImitatorResult = getImitatorResultsForModel(subsetTA, pubReachProp, "pubReach_", imitator_path);

            // Check if the result has to be considered
            // eg. do not consider unreachable if include_unreachable is set to false
            if (considerSubset(privImitatorResult, pubImitatorResult, include_unreach)) {

                // Run polyop and get result
                File polyopResult = getPolyopResultForModel(subsetTA, privImitatorResult, pubImitatorResult, polyop_path);

                if (isOpaqueSubset(polyopResult)) {
                    String constraint = PolyopManip.getConstraint(privImitatorResult.getPath());
                    result.put(subset, constraint);

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

    public static LinkedHashMap<Set<String>, String> getSubsetsToAllow(Set<String> model_actions, LinkedHashMap<Set<String>, String> subsetsToDisable) {
        LinkedHashMap<Set<String>, String> subsetsToAllow = new LinkedHashMap<>();

        for (Map.Entry<Set<String>, String> entry : subsetsToDisable.entrySet()) {
            Set<String> subset = entry.getKey();
            String constraint = entry.getValue();
            subsetsToAllow.put(excludeActions(model_actions, subset), constraint);
        }
        return subsetsToAllow;
    }

    private static Set<String> excludeActions(Set<String> allActions, Set<String> toRemove) {
        Set<String> copy = new HashSet<>(allActions);
        copy.removeAll(toRemove);
        return copy;
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
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static File getImitatorResultsForModel(File model, File propertyFile, String outputPrefix, String imitator_path) {
        String propertyPath = propertyFile.getPath();
        String res_name_without_extension = Params.nameOfResImitatorFile(model.getName(), outputPrefix);

        //Run
        String[] cmd = {imitator_path, model.getPath(), propertyPath, "-output-prefix", Params.pathToOutput + "/" + res_name_without_extension};
        runTerminal(cmd);

        String res_name_with_extension = res_name_without_extension + ".res";

        return new File(Params.pathToOutput + "/" + res_name_with_extension);
    }

    private static File getPolyopResultForModel(File inputModel, File privImitatorResult, File pubImitatorResult, String polyop_path) {
        String privPath = privImitatorResult.getPath();
        String pubPath = pubImitatorResult.getPath();

        String privConstraint = PolyopManip.getConstraint(privPath);
        String pubConstraint = PolyopManip.getConstraint(pubPath);

        //checking if both are reachable then will create a .polyop file
        File polyOpPath = PolyopManip.createPolyopFile(inputModel);

        String content = String.format("equal (%s,%s)", pubConstraint, privConstraint);
        FilesManip.writeToFile(content, polyOpPath, false);

        runTerminal(new String[]{polyop_path, polyOpPath.getPath()});
        String resPath = polyOpPath.getPath() + ".res";
        File result = new File(resPath);
        return result;
    }

    public static File writeActionSubset(File modelFile, LinkedHashMap<Set<String>, String> subsetsToAllow) {
        String outputFileName = Params.nameOfActionSubsetOutput(modelFile);
        File outputFile = FilesManip.createFileNamed(outputFileName);

        for (Map.Entry<Set<String>, String> entry : subsetsToAllow.entrySet()) {
            Set<String> subset = entry.getKey();
            String constraint = entry.getValue();

            StringJoiner joiner = new StringJoiner(", ");
            for (String action : subset) {
                joiner.add(action);
            }

            String constraintOutput = constraint.replace("\n", "");

            FilesManip.addLine(outputFile, "{" + joiner + "} " + constraintOutput);
        }
        return outputFile;
    }
}
