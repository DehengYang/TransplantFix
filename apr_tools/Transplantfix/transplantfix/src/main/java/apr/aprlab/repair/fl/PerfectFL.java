package apr.aprlab.repair.fl;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.search.CuInfo;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.YamlUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class PerfectFL extends Localizer {

    public static final Logger logger = LogManager.getLogger(PerfectFL.class);

    private String patchInfoDir;

    private List<SuspiciousStmt> slList = new ArrayList<>();

    public PerfectFL(String patchInfoDir) {
        this.patchInfoDir = patchInfoDir;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SuspiciousStmt> localize() {
        File buggyLocPath = Paths.get(patchInfoDir, "buggylocs.yaml").toFile();
        if (!buggyLocPath.exists()) {
            ExceptionUtil.raise("buggyLocPath: %s not exits.", buggyLocPath);
        }
        Map<String, Object> buggyChunks = YamlUtil.readYaml(buggyLocPath);
        for (String className : buggyChunks.keySet()) {
            List<Map<String, Object>> chunkList = (List<Map<String, Object>>) buggyChunks.get(className);
            for (Map<String, Object> chunk : chunkList) {
                for (String addOrDel : chunk.keySet()) {
                    List<Integer> lineNoList = (List<Integer>) chunk.get(addOrDel);
                    logger.info("single chunk info ({}): {}", addOrDel, PrintUtil.listToString(lineNoList, className).trim());
                    for (int lineNo : lineNoList) {
                        slList.add(new SuspiciousStmt(className, lineNo));
                    }
                }
            }
        }
        return slList;
    }

    public void toMethodRankingList(List<SuspiciousMethod> standSmList) {
        for (SuspiciousStmt sl : slList) {
            String className = sl.getClassName();
            int lineNo = sl.getLineNo();
            CuInfo cuInfo = SearchTypeHierarchy.classnameToCuInfoMap.get(className);
            if (cuInfo == null) {
                ExceptionUtil.myAssert(Globals.debugMode);
                continue;
            }
            MethodDeclaration md = ASTUtil.findMethodByLineNo(cuInfo.getCompilationUnit(), lineNo);
            if (md == null) {
                logger.warn("loc has no method: {} {}", className, lineNo);
                continue;
            }
            ExceptionUtil.assertNotNull(md);
            SuspiciousMethod sm = new SuspiciousMethod(sl, md, cuInfo.getCompilationUnit());
            int index = standSmList.indexOf(sm);
            if (index >= 0) {
                SuspiciousMethod suspiciousMethod = standSmList.get(index);
                if (!suspiciousMethods.contains(suspiciousMethod)) {
                    suspiciousMethods.add(suspiciousMethod);
                }
            } else {
                logger.warn("suspicious method is not localized by gzoltar: {}, sl: {}", sm, sl);
                if (!suspiciousMethods.contains(sm)) {
                    suspiciousMethods.add(sm);
                }
            }
        }
    }

    public String getPatchInfoDir() {
        return patchInfoDir;
    }

    public List<SuspiciousStmt> getSlList() {
        return slList;
    }
}
