package apr.aprlab.utils.graph.ddg;

import apr.aprlab.utils.graph.ProgramEdge;
import soot.Value;

public class DDGEdge extends ProgramEdge {

    private final Value value;

    DDGEdge(DDGNode f, DDGNode t, Value v) {
        super(f, t);
        value = v;
    }

    public Value getValue() {
        return value;
    }

    public DDGNode getSrc() {
        return (DDGNode) super.getSrc();
    }

    public DDGNode getTgt() {
        return (DDGNode) super.getTgt();
    }
}
