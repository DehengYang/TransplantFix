package apr.aprlab.repair.adapt.patch.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.utils.general.CmdUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.Pair;
import apr.aprlab.utils.general.RegexUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Defects4JValidator {

    public static final Logger logger = LogManager.getLogger(Defects4JValidator.class);

    public static List<String> runOriFailedMethods(List<String> oriFailedMethods) {
        ExceptionUtil.myAssert(Globals.isForD4j2);
        List<String> actualFailed = new ArrayList<String>();
        for (String oriFailed : oriFailedMethods) {
            String cmd = String.format("cd ~/env;\n  ./change-d4j-version.sh 8;\n" + "export JAVA_HOME=\"/home/apr/env/jdk1.8.0_202/\" " + "&& export PATH=\"$JAVA_HOME/bin/:$PATH\" " + "&& cd %s && java -version &&  timeout %ss defects4j test -t %s\n", Globals.buggyDir, Globals.singleTestTimeout, oriFailed.replace("#", "::"));
            String output = CmdUtil.runCmd(cmd);
            if (!output.contains("Failing tests: ")) {
                logger.warn("[runOriFailedMethods] defects4j test may be timeout.");
                actualFailed.add(oriFailed);
            }
            if (output.contains("Failing tests: 1")) {
                actualFailed.add(oriFailed);
            }
        }
        return actualFailed;
    }

    public static List<String> runAll() {
        ExceptionUtil.myAssert(Globals.isForD4j2);
        List<String> actualFailed = new ArrayList<String>();
        String cmd = String.format("cd ~/env;\n  ./change-d4j-version.sh 8;\n" + "export JAVA_HOME=\"/home/apr/env/jdk1.8.0_202/\" " + "&& export PATH=\"$JAVA_HOME/bin/:$PATH\" " + "&& cd %s && java -version &&  timeout %ss defects4j test\n", Globals.buggyDir, Globals.allTestTimeout);
        String output = CmdUtil.runCmd(cmd);
        if (!output.contains("Failing tests: ")) {
            logger.warn("[runAll] defects4j test may be timeout.");
            return null;
        } else {
            List<Pair<String, String>> matches = RegexUtil.findAllPairs(output, Pattern.compile("- (.*)::(.*)"));
            for (Pair<String, String> pair : matches) {
                actualFailed.add(String.format("%s#%s", pair.getLeft(), pair.getRight()));
            }
            return actualFailed;
        }
    }
}
