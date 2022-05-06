package apr.aprlab.repair.fl;

public class SuspiciousStmt extends SuspiciousLocation {

    private int lineNo;

    public SuspiciousStmt(String className, int lineNo) {
        this.className = getName(className);
        this.lineNo = lineNo;
        this.suspValue = -1;
    }

    public SuspiciousStmt(String className, int lineNo, double suspValue) {
        this.className = getName(className);
        this.lineNo = lineNo;
        this.suspValue = suspValue;
    }

    @Override
    public String toString() {
        return String.format("%s:%s, %s, (ef:%s,ep:%s,tf:%s,tp:%s)", this.className, this.lineNo, this.suspValue, this.execFailed, this.execPassed, this.totalFailed, this.totalPassed);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + lineNo;
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
        SuspiciousStmt other = (SuspiciousStmt) obj;
        if (className == null) {
            if (other.className != null)
                return false;
        } else if (!className.equals(other.className))
            return false;
        if (lineNo != other.lineNo)
            return false;
        return true;
    }

    public int getLineNo() {
        return lineNo;
    }
}
