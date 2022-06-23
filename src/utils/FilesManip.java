package utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FilesManip {
    /**
     * Return the name of a file without the extension
     *
     * @param fileName Name of the file (file.ext)
     * @return Name (file)
     */
    public static String getNameWithoutExtension(String fileName) {
        return fileName.replaceFirst("[.][^.]+$", "");
    }

    /**
     * Create a empty file with specified name. Files are created inside of the output directory (in Params)
     *
     * @param fileName File of the file to create
     * @return Object File
     */
    public static File createFileNamed(String fileName) {
        if (!Files.isDirectory(Paths.get(Params.pathToOutput))) {
            try {
                File directory = new File(Params.pathToOutput);
                boolean bol = directory.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        File myObj = new File(Params.pathToOutput, fileName);
        try {
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists:" + myObj.getName());
                String path = myObj.getPath().toString();
                PrintWriter pw = new PrintWriter(path);//erases the content if file already exists
                pw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myObj;
    }

    /**
     * Add a line in a file
     * @param outputFile File in which add the line
     * @param line       Line to add
     */
    public static void addLine(File outputFile, String line) {
        try {
            writeToFile(line + "\n", outputFile, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write something in a file
     *
     * @param s          String to write in the file
     * @param outputFile File to write
     * @param append     true if the function has to append in the file, false if it has to rewrite
     */
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
