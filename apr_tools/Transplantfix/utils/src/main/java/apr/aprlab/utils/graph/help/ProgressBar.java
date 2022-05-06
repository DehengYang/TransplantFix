package apr.aprlab.utils.graph.help;

public class ProgressBar {

    private final double finishPoint;

    private final double barLength;

    public ProgressBar(double finishPoint, int barLength) {
        this.finishPoint = finishPoint;
        this.barLength = barLength;
    }

    public static void main(String[] args) {
        int nodeSize = 10000000;
        ProgressBar bar = new ProgressBar(nodeSize, 50);
        for (int i = 0; i <= nodeSize; i++) if (i % 50 == 0)
            bar.showBarByPoint(i);
    }

    public void showBarByPoint(double currentPoint) {
        double rate = currentPoint / this.finishPoint;
        int barSign = (int) (rate * this.barLength);
        System.out.print("\r");
        System.out.print(makeBarBySignAndLength(barSign) + String.format(" %.2f%%", rate * 100));
    }

    private String makeBarBySignAndLength(int barSign) {
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 1; i <= this.barLength; i++) {
            if (i < barSign) {
                bar.append("-");
            } else if (i == barSign) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        return bar.toString();
    }
}
