package apr.aprlab.utils.general;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DirUtil {

    public static final Logger logger = LogManager.getLogger(FileUtil.class);

    public static void deleteDirectory(String filePath) {
        deleteDirectory(new File(filePath));
    }

    public static void deleteDirectory(File file) {
        if (!file.exists()) {
            logger.info("File does not exist: {}", file.toString());
            return;
        }
        File[] list = file.listFiles();
        if (list != null) {
            for (File temp : list) {
                deleteDirectory(temp);
            }
        }
        if (!file.delete()) {
            ExceptionUtil.raise("Unable to delete file or directory : %s", file);
        }
    }

    public static void mkdirs(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            logger.info("directory is created: {}", dir);
            dir.mkdirs();
        } else {
            logger.info("directory already exists: {}", dir);
        }
    }

    public static void mkdir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.isDirectory()) {
            logger.info("directory is created: {}", dir);
            dir.mkdir();
        } else {
            logger.info("directory already exists: {}", dir);
        }
    }

    public static void delAndMkdir(String dirPath) {
        deleteDirectory(dirPath);
        mkdir(dirPath);
    }

    public static String createNumberedDir(String patchDir, String prefix, String posfix) {
        int i = 0;
        while (Paths.get(patchDir, prefix + i + posfix).toFile().exists()) {
            i++;
        }
        String patchDirPath = Paths.get(patchDir, prefix + i + posfix).toString();
        new File(patchDirPath).mkdir();
        return patchDirPath;
    }

    public static String createNumberedFile(String patchDir, String prefix, String posfix) {
        int i = 0;
        while (Paths.get(patchDir, prefix + i + posfix).toFile().exists()) {
            i++;
        }
        String filePath = Paths.get(patchDir, prefix + i + posfix).toString();
        return filePath;
    }

    public static void copyFileToDirectory(String srcFilePath, String dstDir) {
        try {
            FileUtils.copyFileToDirectory(new File(srcFilePath), new File(dstDir));
        } catch (IOException e) {
            e.printStackTrace();
            ExceptionUtil.raise("copyFileToDirectory failed.");
        }
    }

    public static void removeFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    public static void copyFileToFile(String srcFilePath, String dstFilePath) {
        try {
            FileUtils.copyFile(new File(srcFilePath), new File(dstFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void backupDir(String binJavaDir) {
        if (binJavaDir.endsWith("/")) {
            binJavaDir = binJavaDir.substring(0, binJavaDir.length() - 1);
        }
        String backupDir = binJavaDir + "_bkup";
        if (!new File(backupDir).exists()) {
            try {
                FileUtils.copyDirectory(new File(binJavaDir), new File(backupDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.warn("backupDir already exists: {}", backupDir);
        }
    }

    public static void restoreDir(String binJavaDir) {
        if (binJavaDir.endsWith("/")) {
            binJavaDir = binJavaDir.substring(0, binJavaDir.length() - 1);
        }
        String backupDir = binJavaDir + "_bkup";
        if (new File(backupDir).exists()) {
            try {
                FileUtils.deleteDirectory(new File(binJavaDir));
                FileUtils.copyDirectory(new File(backupDir), new File(binJavaDir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.error("backupDir does not exist: {}", backupDir);
        }
    }

    public static void copyDirectory(String srcDir, String dstDir) {
        try {
            FileUtils.copyDirectory(new File(srcDir), new File(dstDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
