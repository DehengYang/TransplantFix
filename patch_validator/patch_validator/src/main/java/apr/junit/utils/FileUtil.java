package apr.junit.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtil {

    public static void writeToFile(String path, String content) {
        writeToFile(path, content, true);
    }

    public static void writeToFile(String path, String content, boolean append) {
        String dirPath = path.substring(0, path.lastIndexOf("/"));
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println(String.format("%s does not exists, and are created now via mkdirs()", dirPath));
        }
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(path, append));
            output.write(content);
            output.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
