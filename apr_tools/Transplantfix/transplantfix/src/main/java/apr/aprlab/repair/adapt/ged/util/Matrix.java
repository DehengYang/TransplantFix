package apr.aprlab.repair.adapt.ged.util;

public class Matrix {

    private MatrixElement[][] matrix;

    private int row, col;

    private boolean[] coveredRows, coveredCols;

    public Matrix(double[][] m) {
        this.row = m.length;
        if (this.row > 0) {
            this.col = m[0].length;
        } else {
            this.col = this.row;
        }
        this.matrix = new MatrixElement[this.row][this.col];
        for (int i = 0; i < this.row; i++) {
            for (int j = 0; j < this.col; j++) {
                MatrixElement e = new MatrixElement(m[i][j]);
                e.setMyRow(i);
                e.setMyCol(j);
                this.matrix[i][j] = e;
            }
        }
        this.coveredCols = new boolean[this.col];
        this.coveredRows = new boolean[this.row];
        for (int i = 0; i < this.row; i++) {
            this.coveredRows[i] = false;
        }
        for (int j = 0; j < this.col; j++) {
            this.coveredCols[j] = false;
        }
    }

    public MatrixElement get(int r, int c) {
        return this.matrix[r][c];
    }

    public void subtractRowMin() {
        double min;
        for (int i = 0; i < this.row; i++) {
            min = this.getRowMin(i);
            for (int j = 0; j < this.col; j++) {
                this.addValue(i, j, -min);
            }
        }
    }

    public void subtractColMin() {
        double min;
        for (int j = 0; j < this.col; j++) {
            min = this.getColMin(j);
            for (int i = 0; i < this.row; i++) {
                this.addValue(i, j, -min);
            }
        }
    }

    private double getColMin(int col) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.row; i++) {
            if (this.matrix[i][col].getValue() < min) {
                min = this.matrix[i][col].getValue();
            }
        }
        return min;
    }

    private double getRowMin(int row) {
        double min = Double.POSITIVE_INFINITY;
        for (int j = 0; j < this.col; j++) {
            if (this.matrix[row][j].getValue() < min) {
                min = this.matrix[row][j].getValue();
            }
        }
        return min;
    }

    public void starZeros() {
        for (int i = 0; i < this.row; i++) {
            for (int j = 0; j < this.col; j++) {
                if (this.matrix[i][j].getValue() == 0) {
                    if (!starInCOrR(i, j)) {
                        this.matrix[i][j].setStarred(true);
                    }
                }
            }
        }
    }

    private boolean starInCOrR(int row, int col) {
        for (int i = 0; i < this.row; i++) {
            if (this.matrix[i][col].isStarred()) {
                return true;
            }
        }
        for (int j = 0; j < this.col; j++) {
            if (this.matrix[row][j].isStarred()) {
                return true;
            }
        }
        return false;
    }

    public MatrixElement getStarredZeroInR(MatrixElement e) {
        int r = e.getMyRow();
        for (int j = 0; j < this.col; j++) {
            if (this.matrix[r][j].getValue() == 0) {
                if (this.matrix[r][j].isStarred()) {
                    return this.matrix[r][j];
                }
            }
        }
        return null;
    }

    public MatrixElement getStarredZeroInC(MatrixElement e) {
        int c = e.getMyCol();
        for (int i = 0; i < this.row; i++) {
            if (this.matrix[i][c].getValue() == 0) {
                if (this.matrix[i][c].isStarred()) {
                    return this.matrix[i][c];
                }
            }
        }
        return null;
    }

    public MatrixElement getPrimedZeroInC(MatrixElement e) {
        int c = e.getMyCol();
        for (int i = 0; i < this.row; i++) {
            if (this.matrix[i][c].getValue() == 0) {
                if (this.matrix[i][c].isPrimed()) {
                    return this.matrix[i][c];
                }
            }
        }
        return null;
    }

    public MatrixElement getPrimedZeroInR(MatrixElement e) {
        int r = e.getMyRow();
        for (int j = 0; j < this.col; j++) {
            if (this.matrix[r][j].getValue() == 0) {
                if (this.matrix[r][j].isPrimed()) {
                    return this.matrix[r][j];
                }
            }
        }
        return null;
    }

    public void coverStarred() {
        for (int i = 0; i < this.row; i++) {
            for (int j = 0; j < this.col; j++) {
                if (this.matrix[i][j].isStarred()) {
                    this.coverCol(j);
                }
            }
        }
    }

    private void coverCol(int col) {
        this.coveredCols[col] = true;
    }

    public int getCoverNum() {
        int c = 0;
        for (int j = 0; j < this.col; j++) {
            if (this.coveredCols[j]) {
                c++;
            }
        }
        return c;
    }

    public MatrixElement getUncoveredZero() {
        for (int i = 0; i < this.row; i++) {
            if (!this.coveredRows[i]) {
                for (int j = 0; j < this.col; j++) {
                    if (!this.coveredCols[j]) {
                        if (this.matrix[i][j].getValue() == 0) {
                            return this.matrix[i][j];
                        }
                    }
                }
            }
        }
        return null;
    }

    public boolean isStarredCol(MatrixElement e) {
        int c = e.getMyCol();
        for (int i = 0; i < this.row; i++) {
            if (this.matrix[i][c].isStarred()) {
                return true;
            }
        }
        return false;
    }

    public void primeElement(MatrixElement e) {
        e.setPrimed(true);
    }

    public void coverRow(MatrixElement e) {
        int r = e.getMyRow();
        this.coveredRows[r] = true;
    }

    public void unCoverCol(MatrixElement e) {
        int c = e.getMyCol();
        this.coveredCols[c] = false;
    }

    public void unCoverRow(MatrixElement e) {
        int r = e.getMyRow();
        this.coveredRows[r] = false;
    }

    public double getUncoveredMin() {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < this.row; i++) {
            if (!this.coveredRows[i]) {
                for (int j = 0; j < this.col; j++) {
                    if (!this.coveredCols[j]) {
                        if (this.matrix[i][j].getValue() < min) {
                            min = this.matrix[i][j].getValue();
                        }
                    }
                }
            }
        }
        return min;
    }

    public void addToCoveredRows(double e_min) {
        for (int i = 0; i < this.row; i++) {
            if (this.coveredRows[i]) {
                for (int j = 0; j < this.col; j++) {
                    this.addValue(i, j, e_min);
                }
            }
        }
    }

    public void subtractFromUncoveredCols(double e_min) {
        for (int j = 0; j < this.col; j++) {
            if (!this.coveredCols[j]) {
                for (int i = 0; i < this.row; i++) {
                    this.addValue(i, j, -e_min);
                }
            }
        }
    }

    public void unCoverAll() {
        for (int i = 0; i < this.row; i++) {
            this.coveredRows[i] = false;
        }
        for (int j = 0; j < this.col; j++) {
            this.coveredCols[j] = false;
        }
    }

    public void unPrimeAll() {
        for (int j = 0; j < this.col; j++) {
            for (int i = 0; i < this.row; i++) {
                this.matrix[i][j].setPrimed(false);
            }
        }
    }

    public void handleElement(MatrixElement e) {
        e.setStarred(false);
        if (e.isPrimed()) {
            e.setPrimed(false);
            e.setStarred(true);
        }
    }

    private void addValue(int r, int c, double a) {
        double v = this.matrix[r][c].getValue();
        this.matrix[r][c].setValue(v + a);
    }

    public int[][] getStarredIndices(int dim) {
        int[][] indices = new int[dim][2];
        int counter = 0;
        for (int i = 0; i < this.row; i++) {
            for (int j = 0; j < this.col; j++) {
                MatrixElement e = this.matrix[i][j];
                if (e.isStarred()) {
                    indices[counter][0] = i;
                    indices[counter][1] = j;
                    counter++;
                }
            }
        }
        return indices;
    }

    public void printMe() {
        for (int i = 0; i < this.row; i++) {
            for (int j = 0; j < this.col; j++) {
                MatrixElement e = this.matrix[i][j];
                if (this.coveredRows[e.getMyRow()]) {
                    System.out.print("|");
                }
                if (this.coveredCols[e.getMyCol()]) {
                    System.out.print("|");
                }
                if (e.getValue() < Double.POSITIVE_INFINITY) {
                    double temp = ((int) (e.getValue() * 1000.0)) / 1000.0;
                    System.out.print(temp);
                } else {
                    System.out.print("inf");
                }
                if (e.isPrimed()) {
                    System.out.print("'");
                }
                if (e.isStarred()) {
                    System.out.print("*");
                }
                if (this.coveredRows[e.getMyRow()]) {
                    System.out.print("|");
                }
                if (this.coveredCols[e.getMyCol()]) {
                    System.out.print("|");
                }
                System.out.print("\t");
            }
            System.out.println();
        }
        System.out.println("\n");
    }
}
