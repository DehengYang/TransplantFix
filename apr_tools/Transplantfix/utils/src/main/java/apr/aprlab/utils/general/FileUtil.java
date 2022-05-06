package apr.aprlab.utils.general;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtil {

    public static final Logger logger = LogManager.getLogger(FileUtil.class);

    public static String getCanonicalPath(String path) {
        String canonicalPath = null;
        try {
            canonicalPath = Paths.get(path).toFile().getCanonicalPath().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (canonicalPath == null) {
            ExceptionUtil.raise("CanonicalPath of %s is null.", path);
        }
        return canonicalPath;
    }

    public static void writeToFile(String path, String content) {
        writeToFile(path, content, true);
    }

    public static void writeToFile(String path, String content, boolean append) {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(path, append));
            output.write(content);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                output.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static List<String> readFileToList(File file) {
        return readFileToList(file.getAbsolutePath());
    }

    public static List<String> readFileToList(String path) {
        List<String> list = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                list.add(line);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    public static String readFileToStr(File file) {
        return readFileToStr(file.toString());
    }

    public static String readFileToStr(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static String readFileByBytes(String path) {
        return readFileByBytes(path, Charset.forName("UTF-8"));
    }

    static String readFileByBytes(String path, Charset encoding) {
        byte[] encoded = null;
        try {
            encoded = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String content = new String(encoded, encoding);
        return content;
    }

    public static char[] readFileToCharArray(String filePath) {
        return readFileToCharArray(filePath, "UTF-8");
    }

    public static char[] readFileToCharArray(String filePath, String encoding) {
        Reader r = null;
        try {
            r = Files.newBufferedReader(new File(filePath).toPath(), Charset.forName(encoding));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        StringBuilder fileData = new StringBuilder();
        try (BufferedReader br = new BufferedReader(r)) {
            char[] buf = new char[10];
            int numRead = 0;
            while ((numRead = br.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
        } catch (MalformedInputException e) {
            if (encoding.equals("ISO-8859-1")) {
                ExceptionUtil.raise();
            } else {
                logger.warn("MalformedInputException occurs when reading file: {}, now try ISO-8859-1 encoding", filePath);
                return readFileToCharArray(filePath, "ISO-8859-1");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileData.toString().toCharArray();
    }

    public static List<String> readFileToBytes(String path) {
        List<String> list = new ArrayList<>();
        BufferedInputStream bf = null;
        try {
            bf = new BufferedInputStream(new FileInputStream(path));
            int i;
            int length = 0;
            String line = "";
            while ((i = bf.read()) != -1) {
                char c = (char) i;
                line += c;
                if (c == '\n') {
                    list.add(line);
                    line = "";
                }
                length++;
            }
            System.out.println("[readFileBytes] length: " + length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public static String getPathFromClassName(String srcJavaDir, String className) {
        String path = Paths.get(srcJavaDir, className.replace(".", "/") + ".java").toString();
        return path;
    }

    public static String getCanonicalPath(Path path) {
        return getCanonicalPath(path.toString());
    }

    public static String getClassNameFromPath(String srcJavaDirPath, String filePath) {
        String relative = filePath.substring(srcJavaDirPath.length());
        relative = relative.replace("/", ".");
        if (relative.startsWith(".")) {
            relative = relative.substring(1);
        }
        String className = relative.substring(0, relative.length() - ".java".length());
        return className;
    }

    public static <T> void writeToFile(String filePath, List<T> strings, boolean append) {
        writeToFile(filePath, PrintUtil.listToStringForStorage(strings), append);
    }

    public static void writeToFile(Path path, String string) {
        writeToFile(path.toString(), string);
    }

    public static void writeToFile(Path path, String string, boolean append) {
        writeToFile(path.toString(), string, append);
    }

    public static <T> void writeToFile(Path path, List<T> strings, boolean append) {
        writeToFile(path.toString(), PrintUtil.listToStringForStorage(strings), append);
    }
}
