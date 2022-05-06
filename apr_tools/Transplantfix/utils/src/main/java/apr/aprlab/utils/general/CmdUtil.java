package apr.aprlab.utils.general;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CmdUtil {

    public static final Logger logger = LogManager.getLogger(CmdUtil.class);

    public static String runCmd(String cmd) {
        return runCmd(cmd, "rumtime", true, false);
    }

    public static String runCmd(String cmd, boolean printOutput, boolean logging) {
        return runCmd(cmd, "rumtime", printOutput, logging);
    }

    public static String runCmd(String cmd, String execLib, boolean printOutput, boolean logging) {
        if (execLib.equals("commons-io")) {
            return runCmdByCommmonsIO(cmd);
        } else if (execLib.equals("rumtime")) {
            return runCmdByRuntime(cmd, printOutput, logging);
        }
        ExceptionUtil.raise(String.format("%s is not supported now.", execLib));
        return null;
    }

    public static String runCmdByRuntime(String cmd) {
        return runCmdByRuntime(cmd, true, false);
    }

    public static String runCmdByRuntime(String cmd, boolean printOutput, boolean logging) {
        long startTime = System.currentTimeMillis();
        if (logging) {
            logger.info("start to run cmd: {}\n", cmd);
        }
        String stdOutput = null;
        Process proc = null;
        try {
            String[] commands = { "bash", "-c", cmd };
            proc = Runtime.getRuntime().exec(commands);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream(), "UTF-8"));
            stdOutput = getOutputFromReader(stdInput);
            if (printOutput && stdOutput.trim().length() > 0) {
                logger.info("standard output of the cmd: {}", stdOutput);
            }
            String errorOutput = getOutputFromReader(stdError);
            if (errorOutput.strip().length() > 0 && !errorOutput.strip().equals("Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF8")) {
                logger.info("error output of the cmd: {}", errorOutput);
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        String timeCost = TimeUtil.countTime(startTime);
        if (logging) {
            logger.info("time cost of runCmd(): {}s", timeCost);
        }
        return stdOutput;
    }

    private static String getOutputFromReader(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        String curStr = null;
        while ((curStr = br.readLine()) != null) {
            sb.append(curStr).append("\n");
        }
        br.close();
        return sb.toString();
    }

    public static String runCmdByCommmonsIO(String cmd) {
        long startTime = System.currentTimeMillis();
        logger.info("start to run cmd: {}\n", cmd);
        String output = "";
        try {
            String[] commands = { "bash", "-c", cmd };
            Process proc = Runtime.getRuntime().exec(commands);
            String stderr = IOUtils.toString(proc.getErrorStream(), "UTF-8");
            output = IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());
            if (!stderr.equals("")) {
                System.err.println(String.format("Error/Warning occurs:\n %s\n", stderr));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        long timeCost = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("time cost of runCmd(): {}s", timeCost);
        return output;
    }

    public static void runCmdNoPrint(String cmd) {
        try {
            String[] commands = { "bash", "-c", cmd };
            Process proc = Runtime.getRuntime().exec(commands);
            IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
