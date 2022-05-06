package apr.aprlab.utils.graph;

public abstract class ProgramEdge {

    private ProgramNode src;

    private ProgramNode tgt;

    private String label = "";

    public ProgramEdge(ProgramNode src, ProgramNode tgt) {
        this.src = src;
        this.tgt = tgt;
    }

    public ProgramEdge(ProgramNode src, ProgramNode tgt, String label) {
        this.src = src;
        this.tgt = tgt;
        this.label = label;
    }

    public ProgramNode getSrc() {
        return src;
    }

    public ProgramNode getTgt() {
        return tgt;
    }

    public void reverse() {
        ProgramNode tmp = src;
        src = tgt;
        tgt = tmp;
    }

    public String getLabel() {
        return label;
    }
}
