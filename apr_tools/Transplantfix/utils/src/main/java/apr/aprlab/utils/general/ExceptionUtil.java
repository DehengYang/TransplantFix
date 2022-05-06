package apr.aprlab.utils.general;

import java.io.File;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExceptionUtil {

    public static final Logger logger = LogManager.getLogger(ExceptionUtil.class);

    public static int exceptCnt = 0;

    public static void exit() {
        logger.debug("For debugging usage. Exit now.");
        System.exit(-1);
    }

    public static void programExit() {
        logger.debug("Exit.");
        System.exit(1);
    }

    public static void exit(String message) {
        logger.debug(message);
        exit();
    }

    public static void raise() {
        raise("General exception.");
    }

    public static void raise(String message) {
        try {
            throw new Exception(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.error("{}. Exit now.", message);
        System.exit(-1);
    }

    public static void raise(String format, Object... args) {
        String message = String.format(format, args);
        raise(message);
    }

    public static void assertFileExists(String[] filePaths) {
        for (String filePath : filePaths) {
            assertFileExists(filePath);
        }
    }

    public static void assertFileExists(File file) {
        assertFileExists(file.toString());
    }

    public static void assertFileExists(String filePath) {
        if (!new File(filePath).exists()) {
            raise("file does not exists! (filePath: %s)", filePath);
        }
    }

    public static void assertNotNull(Object obj) {
        if (obj == null) {
            raise("obj is null!");
        }
    }

    public static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            raise("obj is null! message: %s", message);
        }
    }

    public static void assertTrue(boolean cond) {
        if (!cond) {
            raise("the condition should be true (but is false).");
        }
    }

    public static void myAssert(boolean cond) {
        assertTrue(cond);
    }

    public static void assertTrue(boolean cond, String message) {
        if (!cond) {
            raise("the condition should be true (but is false).\nmessage: " + message);
        }
    }

    public static void assertFalse(boolean cond) {
        if (cond) {
            raise("the condition should be false (but is true!).");
        }
    }

    public static void assertFalse(boolean cond, String message) {
        if (cond) {
            raise("the condition should be false (but is true!). Error message: %s", message);
        }
    }

    public static void todo() {
        raise("to be implemented!");
    }

    public static void todo(String message) {
        raise("to be implemented: " + message);
    }

    public static <T> void assertNotEmpty(List<T> list) {
        myAssert(!list.isEmpty());
    }
}
