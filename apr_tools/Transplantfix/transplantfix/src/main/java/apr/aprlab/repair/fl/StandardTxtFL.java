package apr.aprlab.repair.fl;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.fl.parser.StandardFLParser;
import apr.aprlab.repair.search.CuInfo;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardTxtFL extends Localizer {

    public static final Logger logger = LogManager.getLogger(StandardTxtFL.class);

    private List<SuspiciousStmt> slList = new ArrayList<>();

    private String flTxtPath;

    public StandardTxtFL(String flTxtPath) {
        this.flTxtPath = flTxtPath;
    }

    @Override
    public List<SuspiciousStmt> localize() {
        ExceptionUtil.assertFileExists(flTxtPath);
        slList = StandardFLParser.readBuggylocFileFromRecoderFl(flTxtPath);
        toMethodRankingList();
        FileUtil.writeToFile(Paths.get(Globals.outputDir, "method_fl.txt"), suspiciousMethods, false);
        return slList;
    }

    public void toMethodRankingList() {
        for (SuspiciousStmt sl : slList) {
            String className = sl.getClassName();
            int lineNo = sl.getLineNo();
            CuInfo cuInfo = SearchTypeHierarchy.classnameToCuInfoMap.get(className);
            if (cuInfo == null) {
                if (!Globals.debugMode) {
                    logger.warn("sl has no cuInfo: {}", sl);
                }
                continue;
            }
            MethodDeclaration md = ASTUtil.findMethodByLineNo(cuInfo.getCompilationUnit(), lineNo);
            if (md == null) {
                continue;
            }
            ExceptionUtil.assertNotNull(md);
            SuspiciousMethod sm = new SuspiciousMethod(sl, md, cuInfo.getCompilationUnit());
            int index = suspiciousMethods.indexOf(sm);
            if (index >= 0) {
                suspiciousMethods.get(index).addSuspiciousStmt(sl);
            } else {
                sm.addSuspiciousStmt(sl);
                suspiciousMethods.add(sm);
            }
        }
    }
}
