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
    private static Set<Set<String>> getSubsetsOfActions(Set<String> actions) {
        Set<Set<String>> subsets = new LinkedHashSet<>();

        String[] set = new String[actions.size()];
        set = actions.toArray(set);
        int n = set.length;
        for (int i = 0; i < (1 << n); i++) {
            Set<String> inner_set = new LinkedHashSet<>();

            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) > 0) {
                    inner_set.add(set[j]);
                }
            }
            subsets.add(inner_set);

        }
        return subsets;
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

    public static ArrayList<File> createSubsetTAs(File editedFile) {
        ArrayList<File> subsetFiles = new ArrayList<>();
        Set<String> actions = ImitatorManip.getActions(editedFile);
        Set<Set<String>> subsets = getSubsetsOfActions(actions);

        for (Set<String> subset : subsets) {
            subsetFiles.add(createSubsetTA(editedFile, subset));
        }

        return subsetFiles;
    }

    public static Set<Set<String>> getOpaqueSubsets(File inputTA, LinkedHashMap<File, File> polyopResults) {
        Set<Set<String>> actionsToAllow = new LinkedHashSet<>();
        Set<String> model_actions = ImitatorManip.getActions(inputTA);

        for (Map.Entry<File, File> entry : polyopResults.entrySet()) {
            File model = entry.getKey();
            File result = entry.getValue();

            try {
                Scanner scanner = new Scanner(result);


                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("yes")) {

                        Set<String> disabler_actions = ImitatorManip.getDisablerActions(model);
                        Set<String> model_copy = new HashSet<>(model_actions);
                        model_copy.removeAll(disabler_actions);
                        actionsToAllow.add(model_copy);
                    }

                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return actionsToAllow;
    }

    /**
     * Run a command in terminal
     *
     * @param command Commadn to run
     */
    private static void runTerminal(String command) {
        try {
            Runtime.getRuntime().exec(command); //TODO: exec is deprecated
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static LinkedHashMap<File, File> getImitatorResultsForModels(ArrayList<File> subsetFiles, File propertyFile, String outputPrefix) {
        LinkedHashMap<File, File> output = new LinkedHashMap<>();
        String propertyPath = propertyFile.getPath();
        for (File model : subsetFiles) {
            String res_name_without_extension = Params.nameOfResImitatorFile(model.getName(), outputPrefix);

            //Run
            String cmd = Params.PathImitator + " " + model.getPath() + " " + propertyPath + " -output-prefix " + Params.pathToOutput + "/" + res_name_without_extension;
            System.out.println("* [RUN] " + cmd);
            runTerminal(cmd);

            String res_name_with_extension = res_name_without_extension + ".res";

            File result = new File(Params.pathToOutput + "/" + res_name_with_extension);
            output.put(model, result);
        }
        return output;
    }

    public static LinkedHashMap<File, File> getPolyopResultsForModels(LinkedHashMap<File, File> privImitatorResults, LinkedHashMap<File, File> pubImitatorResults, boolean include_unreach) {
        LinkedHashMap<File, File> output = new LinkedHashMap<>();
        for (Map.Entry<File, File> entry : privImitatorResults.entrySet()) {
            File privKey = entry.getKey();
            String privVal = entry.getValue().getPath();
            String pubVal = pubImitatorResults.get(privKey).getPath();


            String pubConstraint = PolyopManip.getConstraint(pubVal);
            String privConstraint = PolyopManip.getConstraint(privVal);

            //checking if both are reachable then will create a .polyop file


            File polyOpPath = PolyopManip.createPolyopFile(privKey);
            String content = String.format("equal (%s,%s)", pubConstraint, privConstraint);
            /* String content = "equal (" + pubConstraint + "," + privConstraint + ")";*/
            FilesManip.writeToFile(content, polyOpPath, false);

            runTerminal(Params.PathPolyop + " " + polyOpPath);
            String resPath = polyOpPath.getPath() + ".res";
            if (include_unreach) {
                File result = new File(resPath);
                output.put(privKey, result);
            } else {
                if (!privConstraint.equals("False") && !pubConstraint.equals("False")) {
                    File result = new File(resPath);
                    output.put(privKey, result);
                }
            }


        }
        return output;
    }
}
