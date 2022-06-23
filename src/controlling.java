/****************
 * AUTHOR: Shapagat Boilat
 * Modifications by: Dylan Marinho
 ****************/

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.Keyword;
import utils.Params;

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
        if(args.length == 1) { // case where only one argument is set: use default value for unreach
            file_name = args[0];
            include_unreach = Params.default_unreach_option;
        } else if (args.length == 2 && ((args[1].equals("include-unreach") || args[1].equals("exclude-unreach")))) {
            file_name = args[0];
            include_unreach = args[1].equals("include-unreach");
        } else {
            throw new Error("Two program arguments are needed! Syntax: controlling [input_file.imi] ?[include-unreach | exclude-unreach]");
        }



        File inputTA = new File(file_name);
        /* String PathImitator = "imitator";
        String PathPolyop = "polyop";*/
        String PathImitator = "/home/shapagat/Downloads/imitator-3.2.0/bin/imitator";
        String PathPolyop = "/home/shapagat/Downloads/polyop";


        File editedTA = createEditedTA(inputTA);
        ArrayList<File> subsetTAs = createSubsetTAs(editedTA);
        File privReachProp = createReachFile(editedTA, true);
        File pubReachProp = createReachFile(editedTA, false);


        LinkedHashMap<File, File> privImitatorResults = getImitatorResultsForModels(subsetTAs, privReachProp, PathImitator, "privReach_");
        LinkedHashMap<File, File> pubImitatorResults = getImitatorResultsForModels(subsetTAs, pubReachProp, PathImitator, "pubReach_");

        LinkedHashMap<File, File> PolyopResults = getPolyopResultsForModels(privImitatorResults, pubImitatorResults, PathPolyop, include_unreach);
        Set<Set<String>> subsetsToAllow = getOpaqueSubsets(inputTA, PolyopResults);
        System.out.println("Following subsets of actions make the system fully opaque:");
        for (Set<String> subset : subsetsToAllow) {

            StringJoiner joiner = new StringJoiner(", ");
            for (String action : subset) {
                joiner.add(action);
            }
            System.out.printf("{%s}%n", joiner);
        }
    }

    private static String getName(File inputTA) {
        String automatonName = "";
        try {
            Scanner scanner = new Scanner(inputTA);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(Keyword.AUTOMATON.toString())) {
                    String pattern = String.format("%s ((.|\\n)*)", Keyword.AUTOMATON);
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find()) {
                        automatonName = m.group(1);
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return automatonName;
    }

    private static Set<Set<String>> getOpaqueSubsets(File inputTA, LinkedHashMap<File, File> polyopResults) {
        Set<Set<String>> actionsToAllow = new LinkedHashSet<>();
        Set<String> model_actions = getActions(inputTA);

        for (Map.Entry<File, File> entry : polyopResults.entrySet()) {
            File model = entry.getKey();
            File result = entry.getValue();

            try {
                Scanner scanner = new Scanner(result);


                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("yes")) {

                        Set<String> disabler_actions = getDisablerActions(model);
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
   /* private boolean isCommentedOut(String line){
        if()
        return false;
    }*/

    private static String getNameWithoutExtension(String fileName) {
        return fileName.replaceFirst("[.][^.]+$", "");
    }

    private static Set<String> getDisablerActions(File model) {
        boolean disabler_found = false;
        Set<String> result = new LinkedHashSet<>();
        try {
            Scanner scanner = new Scanner(model);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(Keyword.SYNCLABS.toString()) && disabler_found) {
                    String pattern = String.format("%s:((.|\\n)*);", Keyword.SYNCLABS);


                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find()) {
                        String[] temp_split = m.group(1).split(",");

                        for (String s : temp_split) {
                            result.add(s.replaceAll("[^A-Za-z\\d]", ""));
                        }
                        break;
                    }

                } else if (line.contains("disabler")) {
                    disabler_found = true;
                }

            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }


        return result;
    }

    private static LinkedHashMap<File, File> getPolyopResultsForModels(LinkedHashMap<File, File> privImitatorResults, LinkedHashMap<File, File> pubImitatorResults, String PathPolyop, boolean include_unreach) {
        LinkedHashMap<File, File> output = new LinkedHashMap<>();
        for (Map.Entry<File, File> entry : privImitatorResults.entrySet()) {
            File privKey = entry.getKey();
            String privVal = entry.getValue().getPath();
            String pubVal = pubImitatorResults.get(privKey).getPath();


            String pubConstraint = getConstraint(pubVal);
            String privConstraint = getConstraint(privVal);

            //checking if both are reachable then will create a .polyop file


            File polyOpPath = createPolyopFile(privKey);
            String content = String.format("equal (%s,%s)", pubConstraint, privConstraint);
            /* String content = "equal (" + pubConstraint + "," + privConstraint + ")";*/
            writeToFile(content, polyOpPath, false);

            runTerminal(PathPolyop + " " + polyOpPath);
            String resPath = polyOpPath.getPath() + ".res";
            if(include_unreach){
                File result = new File(resPath);
                output.put(privKey, result);
            }else{
                if (!privConstraint.equals("False") && !pubConstraint.equals("False")) {
                    File result = new File(resPath);
                    output.put(privKey, result);
                }
            }



        }
        return output;
    }

    private static File createReachFile(File inputFile, boolean isPrivate) {
        String automatonName = getName(inputFile);
        String inputName = getNameWithoutExtension(inputFile.getName());
        String fileName;
        if (isPrivate) {
            fileName = String.format("reachPriv_%s.imiprop",
                    inputName);
        } else {
            fileName = String.format("reachPub_%s.imiprop",
                    inputName);
        }
        File myObj = createFileNamed(fileName);
        try {
            String output;
            if (isPrivate) {
                output = String.format("property := #synth EF(loc[%s] = qf & visited_qpriv = True);", automatonName);

            } else {
                output = String.format("property := #synth EF(loc[%s] = qf & visited_qpriv = False);", automatonName);
            }

            FileWriter fileWriter = new FileWriter(myObj);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(output);

            printWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return myObj;
    }

    private static Set<String> getActions(File inputFile) {
        Set<String> result = new LinkedHashSet<>();
        try {
            Scanner scanner = new Scanner(inputFile);


            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(Keyword.SYNCLABS.toString())) {
                    String pattern = String.format("%s:((.|\\n)*);", Keyword.SYNCLABS);
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find()) {


                        String[] temp_split = m.group(1).split(",");

                        for (String s : temp_split) {
                            result.add(s.replaceAll("[^A-Za-z\\d]", ""));
                        }
                        break;
                    }

                }

            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }


        return result;
    }

    private static File createEditedTA(File inputFile) {

        String inputName = getNameWithoutExtension(inputFile.getName());
        String outputName = String.format("%s_edited.imi", inputName);
        File outputFile = createFileNamed(outputName);
        try {
            Scanner scanner = new Scanner(inputFile);
            String init_keyword = "init := {";
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(Keyword.CLOCK.toString())) {
                    editVariables(outputFile);
                } else if (line.contains(String.format("%s %s;", Keyword.GOTO, Keyword.LOC_FINAL)) || line.contains(String.format("%s %s;", Keyword.GOTO, Keyword.LOC_PRIV))) {
                    editEdges(outputFile, line);
                } else if (line.contains(init_keyword)) {
                    editDisablerAutomaton(outputFile);

                } else if (line.contains(Keyword.DISCRETE.toString()) || line.contains(Keyword.CONTINUOUS.toString())) {
                    editInit(outputFile, line);
                } else {
                    addLine(outputFile, line);
                }
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }


        return outputFile;
    }


    private static void editVariables(File outputFile) {

        addLine(outputFile, String.format("\t%s,", Keyword.T_ABS));
        addLine(outputFile, String.format("\t\t: %s;", Keyword.CLOCK));
        addLine(outputFile, String.format("\t%s,", Keyword.P_ABS));
        addLine(outputFile, String.format("\t\t: %s;", Keyword.PARAMETER));
        addLine(outputFile, String.format("\t%s,", Keyword.VISITED_PRIV));
        addLine(outputFile, String.format("\t\t: %s;", Keyword.BOOL));

    }

    private static void writeToFile(String s, File outputFile, boolean append) {
        try {
            FileWriter fw = new FileWriter(outputFile, append);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void showErrorMessage() {
        System.out.println("An error occurred.");
    }

    private static void editEdges(File outputFile, String line) {
        String goto_qf_keyword = "goto qf;";
        String goto_qpriv_keyword = "goto qpriv;";
        try {
            if (line.contains(goto_qf_keyword)) {
                String[] goto_line = line.split(" ");
                StringBuilder ouput_goto_line = new StringBuilder();
                for (String s : goto_line) {

                    if (!s.equals(Keyword.SYNC.toString())) {

                        ouput_goto_line.append(s).append(" ");
                    } else {
                        ouput_goto_line.append(" & ").append(Keyword.T_ABS).append(" = ").append(Keyword.P_ABS).append(" ").append(Keyword.SYNC).append(" ");
                    }
                }
                addLine(outputFile, ouput_goto_line.toString());
            } else if (line.contains(goto_qpriv_keyword)) {
                if (!line.contains(Keyword.DO.toString())) {
                    String[] goto_line = line.split(" ");
                    StringBuilder ouput_goto_line = new StringBuilder();

                    for (int i = 0; i < goto_line.length - 1; i++) {
                        if (!(goto_line[i] + " " + goto_line[i + 1]).equals(goto_qpriv_keyword)) {
                            ouput_goto_line.append(goto_line[i]).append(" ");
                        } else {
                            ouput_goto_line.append(Keyword.DO).append(String.format(" {%s := True} ", Keyword.VISITED_PRIV)).append(goto_qpriv_keyword);
                        }
                    }

                    writeToFile(ouput_goto_line + "\n", outputFile, true);
                } else {
                    String pattern = "\\{(.*?)}";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find()) {
                        System.out.println(m.group(1));

                        String[] goto_line = line.split(" ");
                        StringBuilder ouput_goto_line = new StringBuilder();
                        boolean do_found = false;
                        for (String s : goto_line) {

                            if (s.equals(Keyword.DO.toString())) {
                                do_found = true;
                                ouput_goto_line.append((String.format("%s {%s, %s := True} %s", Keyword.DO, m.group(1), Keyword.VISITED_PRIV, goto_qpriv_keyword)));
                            } else if (!do_found) {
                                ouput_goto_line.append(s).append(" ");
                            }


                        }

                        writeToFile(ouput_goto_line + "\n", outputFile, true);
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void editDisablerAutomaton(File outputFile) {
        String init_keyword = "init := {";
        String output_line = "(************************************************************)\n" +
                "  automaton disabler\n" +
                "(*** NOTE: the purpose of this automaton is to \"disable\" some actions by just declaring them as \"synclabs\" and subsequently NOT using them ***)\n" +
                "(************************************************************)\n" +
                "\n" +
                "(*** TAG begin synclabs ***)\n" +
                "synclabs: ;\n" +
                "(*** TAG end synclabs ***)\n" +
                "\n" +
                "loc dummy: invariant True\n" +
                "\n" +
                "\n" +
                "end (* disabled *)";
        try {
            addLine(outputFile, output_line);
            addLine(outputFile, init_keyword);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void editInit(File outputFile, String line) {
        String init_dummy_disc = String.format("\t\tloc[disabler] := dummy,\n\t\t%s := False,\n", Keyword.VISITED_PRIV);
        String init_dummy_cont = String.format("\t\t& %s = 0\n\t\t& %s >= 0\n", Keyword.T_ABS, Keyword.P_ABS);
        try {
            if (line.contains(Keyword.DISCRETE.toString())) {
                addLine(outputFile, line);
                writeToFile(init_dummy_disc, outputFile, true);

            } else if (line.contains(Keyword.CONTINUOUS.toString())) {
                addLine(outputFile, line);
                writeToFile(init_dummy_cont, outputFile, true);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void addLine(File outputFile, String line) {
        try {
            writeToFile(line + "\n", outputFile, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public static File createFileNamed(String fileName) {
        File myObj = new File(fileName);
        try {
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
                PrintWriter pw = new PrintWriter(fileName);//erases the content if file already exists
                pw.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return myObj;
    }


    public static File createPolyopFile(File modelFile) {

        String cwd = Path.of("").toAbsolutePath().toString();
        return createFileNamed(cwd + "/equality_" + getNameWithoutExtension(modelFile.getName()) + ".polyop");
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

    public static LinkedHashMap<File, File> getImitatorResultsForModels(ArrayList<File> subsetFiles, File propertyFile, String PathImitator, String ouputPrefix) {
        LinkedHashMap<File, File> output = new LinkedHashMap<>();
        String propertyPath = propertyFile.getPath();
        for (File model : subsetFiles) {
            String fileName = getNameWithoutExtension(model.getName());

            //System.out.println(PathImitator+ " "+val + " " + privPath + " -output-prefix privReach_"+key);
            runTerminal(PathImitator + " " + model.getPath() + " " + propertyPath + " -output-prefix " + ouputPrefix + fileName);
            String cwd = Path.of("").toAbsolutePath().toString();
            String resPath = cwd + "/" + ouputPrefix + fileName + ".res";
            File result = new File(resPath);
            output.put(model, result);

        }
        return output;
    }


    public static ArrayList<File> createSubsetTAs(File editedFile) {
        ArrayList<File> subsetFiles = new ArrayList<>();
        Set<String> actions = getActions(editedFile);
        Set<Set<String>> subsets = getSubsetsOfActions(actions);

        for (Set<String> subset : subsets) {
            subsetFiles.add(createSubsetTA(editedFile, subset));
        }


        return subsetFiles;
    }

    private static File createSubsetTA(File inputFile, Set<String> subset) {
        StringJoiner joiner = new StringJoiner("_");
        StringJoiner output_line = new StringJoiner(",");
        for (String action : subset) {
            joiner.add(action);
            output_line.add(action);
        }

        String fileNameWithoutExtension = getNameWithoutExtension(inputFile.getName());
        String fileName = String.format("%s_%s.imi", fileNameWithoutExtension, joiner);
        File outputFile = createFileNamed(fileName);

        try {
            Scanner scanner = new Scanner(inputFile);
            boolean disabler_found = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();


                if (disabler_found) {
                    if (line.contains(Keyword.SYNCLABS+":")) {
                        addLine(outputFile, String.format("%s: %s;", Keyword.SYNCLABS, output_line));

                    } else {
                        addLine(outputFile, line);
                    }
                } else {
                    if (line.contains("disabler")) {
                        disabler_found = true;
                        addLine(outputFile, line);
                    } else {
                        addLine(outputFile, line);
                    }
                }

            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return outputFile;
    }

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


    private static void runTerminal(String command) {
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
