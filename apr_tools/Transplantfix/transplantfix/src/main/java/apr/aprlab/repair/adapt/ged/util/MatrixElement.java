package apr.aprlab.repair.adapt.ged.util;

public class MatrixElement {

    private double value;

    private boolean starred, primed;

    private int myRow, myCol;

    public int getMyCol() {
        return myCol;
    }

    public void setMyCol(int myCol) {
        this.myCol = myCol;
    }

    public int getMyRow() {
        return myRow;
    }

    public void setMyRow(int myRow) {
        this.myRow = myRow;
    }

    public MatrixElement(double value) {
        this.value = value;
    }

    public boolean isPrimed() {
        return primed;
    }

    public void setPrimed(boolean primed) {
        this.primed = primed;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
