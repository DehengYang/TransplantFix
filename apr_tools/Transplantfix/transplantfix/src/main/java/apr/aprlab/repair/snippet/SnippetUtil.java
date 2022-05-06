package apr.aprlab.repair.snippet;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import apr.aprlab.repair.adapt.entities.VariableDef;

public class SnippetUtil {

    public static final Logger logger = LogManager.getLogger(SnippetUtil.class);

    public static void checkVarDefConsistency(List<VariableDef> vds) {
        String curDefType = vds.get(0).getType();
        for (VariableDef vd : vds) {
            if (!vd.getType().equals(curDefType)) {
                logger.warn("inconsistent vars are detected.");
            }
        }
    }
}
