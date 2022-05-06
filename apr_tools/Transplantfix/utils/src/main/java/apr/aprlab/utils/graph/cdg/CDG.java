package apr.aprlab.utils.graph.cdg;

import apr.aprlab.utils.graph.ProgramGraph;
import soot.SootMethod;

public class CDG extends ProgramGraph {

    public CDG(SootMethod mtd) {
        super(mtd);
    }

    private final boolean failed = false;

    public boolean isFailed() {
        return failed;
    }
}
