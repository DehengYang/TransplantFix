package apr.aprlab.repair.fl;

import java.util.BitSet;
import java.util.List;
import apr.aprlab.repair.fl.metric.Ochiai;
import apr.aprlab.repair.fl.parser.TestCase;
import apr.aprlab.utils.general.ExceptionUtil;

public abstract class SuspiciousLocation {

    protected String className;

    protected int execPassed;

    protected int execFailed;

    protected int totalPassed;

    protected int totalFailed;

    protected double suspValue;

    protected BitSet coverage;

    public String getName(String fullClassName) {
        if (fullClassName.contains("$")) {
            return fullClassName.split("\\$")[0];
        } else {
            return fullClassName;
        }
    }

    private double calculateSuspicious() {
        suspValue = new Ochiai().value(execFailed, execPassed, totalFailed - execFailed, totalPassed - execPassed);
        return suspValue;
    }

    public void calculateSuspicious(List<TestCase> testList, int totalPassedCnt, int totalFailedCnt) {
        int executedPassedCount = 0;
        int executedFailedCount = 0;
        for (int j = coverage.nextSetBit(0); j >= 0; j = coverage.nextSetBit(j + 1)) {
            if (j == Integer.MAX_VALUE) {
                ExceptionUtil.raise();
                break;
            }
            TestCase tr = testList.get(j);
            if (tr.isSuccessful()) {
                executedPassedCount++;
            } else {
                executedFailedCount++;
            }
        }
        setExecFailed(executedFailedCount);
        setExecPassed(executedPassedCount);
        setTotalFailed(totalFailedCnt);
        setTotalPassed(totalPassedCnt);
        calculateSuspicious();
    }

    public double getSuspValue() {
        return suspValue;
    }

    public String getClassName() {
        return className;
    }

    public int getTotalPassed() {
        return totalPassed;
    }

    public void setTotalPassed(int totalPassed) {
        this.totalPassed = totalPassed;
    }

    public int getTotalFailed() {
        return totalFailed;
    }

    public void setTotalFailed(int totalFailed) {
        this.totalFailed = totalFailed;
    }

    public int getExecPassed() {
        return execPassed;
    }

    public void setExecPassed(int execPassed) {
        this.execPassed = execPassed;
    }

    public int getExecFailed() {
        return execFailed;
    }

    public void setExecFailed(int execFailed) {
        this.execFailed = execFailed;
    }

    public void setCoverage(BitSet coverage) {
        this.coverage = coverage;
    }

    public BitSet getCoverage() {
        return coverage;
    }

    public void setSuspValue(double suspValue) {
        this.suspValue = suspValue;
    }
}
