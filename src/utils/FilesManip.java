package utils;

import java.io.*;

public class FilesManip {
    /** Return the name of a file without the extension
     * @param fileName Name of the file (file.ext)
     * @return Name (file)
     * */
    public static String getNameWithoutExtension(String fileName) {
        return fileName.replaceFirst("[.][^.]+$", "");
    }

    /** Create a empty file with specified name
     * @param fileName File of the file to create
     * @return Object File
     * */
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

    /** Add a line in a file
     * @param outputFile File in which add the line
     * @param line Line to add
     * */
    public static void addLine(File outputFile, String line) {
        try {
            writeToFile(line + "\n", outputFile, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Write something in a file
     * @param s String to write in the file
     * @param outputFile File to write
     * @param append true if the function has to append in the file, false if it has to rewrite*/
    public static void writeToFile(String s, File outputFile, boolean append) {
        try {
            FileWriter fw = new FileWriter(outputFile, append);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
