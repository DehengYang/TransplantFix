package apr.aprlab.repair.fl;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.fl.parser.StandardFLParser;
import apr.aprlab.repair.fl.parser.TestCase;
import apr.aprlab.repair.search.CuInfo;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardFL extends Localizer {

    public static final Logger logger = LogManager.getLogger(StandardFL.class);

    private String localizeInfoDir;

    private List<SuspiciousStmt> slList = new ArrayList<>();

    private List<TestCase> testList = new ArrayList<TestCase>();

    public StandardFL(String workingDir) {
        this.localizeInfoDir = workingDir;
    }

    @Override
    public List<SuspiciousStmt> localize() {
        String spec_file = Paths.get(localizeInfoDir, "stmt_list.txt").toString();
        String coverage_file = Paths.get(localizeInfoDir, "coverage.txt").toString();
        String test_file = Paths.get(localizeInfoDir, "test_method_list.txt").toString();
        ExceptionUtil.assertFileExists(new String[] { spec_file, coverage_file, test_file });
        testList = StandardFLParser.readTestFile(test_file);
        slList = StandardFLParser.readStmtFile(spec_file);
        Pair<Integer, Integer> pair = StandardFLParser.parseMatrixFile(coverage_file, slList, testList);
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
                sm.setExecFailed(sl.getExecFailed());
                sm.setExecPassed(sl.getExecPassed());
                sm.setTotalFailed(sl.getTotalFailed());
                sm.setTotalPassed(sl.getTotalPassed());
                sm.setSuspValue(sl.getSuspValue());
                suspiciousMethods.add(sm);
            }
        }
    }

    public String getLocalizeInfoDir() {
        return localizeInfoDir;
    }

    public List<SuspiciousStmt> getSlList() {
        return slList;
    }

    public List<TestCase> getTestList() {
        return testList;
    }
}
