package apr.aprlab.repair.adapt.patch.validate.strategy;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;

public class TestUtil {

    public static List<String> obtainOriFailedMethods(String buggyDir) {
        String d4jPropertyPath = Paths.get(buggyDir, "defects4j.build.properties").toString();
        List<String> propList = FileUtil.readFileToList(d4jPropertyPath);
        for (String prop : propList) {
            if (prop.startsWith("d4j.tests.trigger=")) {
                prop = prop.substring("d4j.tests.trigger=".length());
                String[] failedMethods = prop.replace("::", "#").split(",");
                return new ArrayList<String>(Arrays.asList(failedMethods));
            }
        }
        ExceptionUtil.raise();
        return null;
    }
}
