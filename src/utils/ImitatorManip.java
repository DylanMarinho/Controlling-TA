package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImitatorManip {

    /**
     * Add needed variables in the PTA
     *
     * @param outputFile File of the output of the edit
     */
    private static void editVariables(File outputFile) {
        FilesManip.addLine(outputFile, String.format("\t%s,", Keyword.T_ABS));
        FilesManip.addLine(outputFile, String.format("\t\t: %s;", Keyword.CLOCK));
        FilesManip.addLine(outputFile, String.format("\t%s,", Keyword.P_ABS));
        FilesManip.addLine(outputFile, String.format("\t\t: %s;", Keyword.PARAMETER));
        FilesManip.addLine(outputFile, String.format("\t%s,", Keyword.VISITED_PRIV));
        FilesManip.addLine(outputFile, String.format("\t\t: %s;", Keyword.BOOL));
    }

    //TODO: Description
    private static void editEdges(File outputFile, String line, String loc_final, String loc_priv) {
        String goto_qf_keyword = "goto "+ loc_final +";";
        String goto_qpriv_keyword = "goto "+ loc_priv +";";
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
                FilesManip.addLine(outputFile, ouput_goto_line.toString());
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
                    FilesManip.writeToFile(ouput_goto_line + "\n", outputFile, true);
                } else {
                    String pattern = "\\{(.*?)}";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(line);
                    if (m.find()) {
                        //System.out.println(m.group(1));

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

                        FilesManip.writeToFile(ouput_goto_line + "\n", outputFile, true);
                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: Description
    private static void editDisablerAutomaton(File outputFile) {
        String init_keyword = "init := {";
        String output_line = "(************************************************************)\n" + "  automaton disabler\n" + "(*** NOTE: the purpose of this automaton is to \"disable\" some actions by just declaring them as \"synclabs\" and subsequently NOT using them ***)\n" + "(************************************************************)\n" + "\n" + "(*** TAG begin synclabs ***)\n" + "synclabs: ;\n" + "(*** TAG end synclabs ***)\n" + "\n" + "loc dummy: invariant True\n" + "\n" + "\n" + "end (* disabled *)";
        try {
            FilesManip.addLine(outputFile, output_line);
            FilesManip.addLine(outputFile, init_keyword);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    //TODO: Description
    private static void editInit(File outputFile, String line) {
        String init_dummy_disc = String.format("\t\tloc[disabler] := dummy,\n\t\t%s := False,\n", Keyword.VISITED_PRIV);
        String init_dummy_cont = String.format("\t\t& %s = 0\n\t\t& %s >= 0\n", Keyword.T_ABS, Keyword.P_ABS);
        try {
            if (line.contains(Keyword.DISCRETE.toString())) {
                FilesManip.addLine(outputFile, line);
                FilesManip.writeToFile(init_dummy_disc, outputFile, true);

            } else if (line.contains(Keyword.CONTINUOUS.toString())) {
                FilesManip.addLine(outputFile, line);
                FilesManip.writeToFile(init_dummy_cont, outputFile, true);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: explicit what is a "edited TA"

    /**
     * From file.imi, create file_edited.imi where ...
     *
     * @param inputFile input imi file
     * @return variable to the edited file
     */
    public static File createEditedTA(File inputFile, String loc_final, String loc_priv) {
        String outputName = Params.nameOfEditedTA(inputFile);
        File outputFile = FilesManip.createFileNamed(outputName);

        try {
            Scanner scanner = new Scanner(inputFile);
            String init_keyword = "init := {"; //TODO: Move to KEYWORD
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(Keyword.CLOCK.toString())) {
                    editVariables(outputFile);
                } else if (line.contains(String.format("%s %s;", Keyword.GOTO, loc_final)) || line.contains(String.format("%s %s;", Keyword.GOTO, loc_priv))) {
                    editEdges(outputFile, line, loc_final, loc_priv);
                } else if (line.contains(init_keyword)) {
                    editDisablerAutomaton(outputFile);
                } else if (line.contains(Keyword.DISCRETE.toString()) || line.contains(Keyword.CONTINUOUS.toString())) {
                    editInit(outputFile, line);
                } else {
                    FilesManip.addLine(outputFile, line);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return outputFile;
    }

    public static Set<String> getActions(File inputFile) {
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

    public static Set<String> getDisablerActions(File model) {
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

    /** Create a property file for reachability
     * @param inputFile File with the PTA to check
     * @param isPrivate Has reachability to be performed for public or private runs?
     * @return File object*/
    public static File createReachFile(File inputFile, boolean isPrivate) {
        String automatonName = ImitatorManip.getName(inputFile);
        String fileName;
        if (isPrivate) {
            fileName = Params.nameOfPrivateReachProperty(inputFile);
        } else {
            fileName = Params.nameOfPublicReachProperty(inputFile);
        }
        File myObj = FilesManip.createFileNamed(fileName);
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
}
