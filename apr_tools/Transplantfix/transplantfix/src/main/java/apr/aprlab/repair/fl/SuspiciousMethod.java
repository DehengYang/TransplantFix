package apr.aprlab.repair.fl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import apr.aprlab.repair.fl.parser.TestCase;
import apr.aprlab.utils.ast.Range;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SuspiciousMethod extends SuspiciousLocation {

    public static final Logger logger = LogManager.getLogger(SuspiciousMethod.class);

    private MethodDeclaration methodDeclaration;

    private Range range;

    private String methodName;

    private List<SuspiciousStmt> suspiciousStmts = new ArrayList<SuspiciousStmt>();

    private List<String> coveredTestCases = new ArrayList<String>();

    public SuspiciousMethod(SuspiciousStmt sl, MethodDeclaration md, CompilationUnit cu) {
        this.className = sl.getClassName();
        this.methodDeclaration = md;
        this.range = RangeUtil.getRange(cu, md);
        this.coverage = sl.getCoverage();
        this.methodName = md.getName().toString();
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public Range getRange() {
        return range;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((range == null) ? 0 : range.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SuspiciousMethod other = (SuspiciousMethod) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (range == null) {
            if (other.range != null)
                return false;
        } else if (!range.equals(other.range))
            return false;
        return true;
    }

    public void mergeCoverage(BitSet coverage2) {
        coverage.or(coverage2);
    }

    @Override
    public String toString() {
        return String.format("%s:%s, %s, (ef:%s,ep:%s,tf:%s,tp:%s)", this.className, methodName, this.suspValue, this.execFailed, this.execPassed, this.totalFailed, this.totalPassed);
    }

    public void addSuspiciousStmt(SuspiciousStmt sl) {
        if (!suspiciousStmts.contains(sl)) {
            suspiciousStmts.add(sl);
        } else {
            logger.warn("repeated susp stmt: {}", sl);
        }
    }

    public List<String> getCoveredTestCases() {
        return coveredTestCases;
    }

    public void setCoveredTestCases(List<TestCase> testList) {
        ExceptionUtil.myAssert(coveredTestCases.isEmpty());
        for (int j = coverage.nextSetBit(0); j >= 0; j = coverage.nextSetBit(j + 1)) {
            if (j == Integer.MAX_VALUE) {
                ExceptionUtil.raise();
                break;
            }
            TestCase tr = testList.get(j);
            coveredTestCases.add(tr.getTestName());
        }
    }
}
