package apr.aprlab.utils.ast;

public class Range {

    private int startLineNo;

    private int endLineNo;

    public Range(int startLineNo, int endLineNo) {
        this.startLineNo = startLineNo;
        this.endLineNo = endLineNo;
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", startLineNo, endLineNo);
    }

    public int getStartLineNo() {
        return startLineNo;
    }

    public int getEndLineNo() {
        return endLineNo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + endLineNo;
        result = prime * result + startLineNo;
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
        Range other = (Range) obj;
        if (endLineNo != other.endLineNo)
            return false;
        if (startLineNo != other.startLineNo)
            return false;
        return true;
    }
}
