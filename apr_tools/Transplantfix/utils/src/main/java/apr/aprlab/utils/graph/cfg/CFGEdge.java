package apr.aprlab.utils.graph.cfg;

import apr.aprlab.utils.graph.ProgramEdge;
import apr.aprlab.utils.graph.ProgramNode;

public class CFGEdge extends ProgramEdge {

    public CFGEdge(ProgramNode src, ProgramNode tgt) {
        super(src, tgt);
    }

    public CFGEdge(ProgramNode src, ProgramNode tgt, String label) {
        super(src, tgt, label);
    }
}
