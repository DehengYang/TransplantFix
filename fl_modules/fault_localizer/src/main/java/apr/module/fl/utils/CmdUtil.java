package apr.module.fl.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;

public class CmdUtil {

    public static String runCmd(String cmd) {
        String output = "";
        try {
            String[] commands = { "bash", "-c", cmd };
            Process proc = Runtime.getRuntime().exec(commands);
            String stderr = IOUtils.toString(proc.getErrorStream(), Charset.defaultCharset());
            output = IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());
            if (!stderr.equals("")) {
                System.err.println(String.format("Error/Warning occurs:\n %s\n", stderr));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return output;
    }

    public static void runCmdNoOutput(String cmd) {
        try {
            String[] commands = { "bash", "-c", cmd };
            Process proc = Runtime.getRuntime().exec(commands);
            IOUtils.toString(proc.getInputStream(), Charset.defaultCharset());
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static String runCmd2(String cmd) {
        StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", cmd);
            Process process = processBuilder.start();
            String stdout = IOUtils.toString(process.getInputStream(), Charset.defaultCharset());
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
                output.append(line + "\n");
            }
            String error = "";
            while ((line = stdError.readLine()) != null) {
                error += line + "\n";
            }
            if (!error.equals("")) {
                System.err.println(String.format("Error/Warning occurs:\n %s\n", error));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return output.toString();
    }
}
