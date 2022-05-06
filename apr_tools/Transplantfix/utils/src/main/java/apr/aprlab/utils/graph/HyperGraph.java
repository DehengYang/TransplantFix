package apr.aprlab.utils.graph;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import apr.aprlab.utils.graph.cdg.CDG;
import apr.aprlab.utils.graph.cfg.CFG;
import apr.aprlab.utils.graph.ddg.DDG;
import apr.aprlab.utils.graph.printer.GraphPrinter;
import apr.aprlab.utils.tree.ASTTree;
import soot.SootMethod;

public class HyperGraph extends ProgramGraph {

    public boolean failed;

    private DDG ddg;

    private CDG cdg;

    private CFG cfg;

    private ASTTree astg;

    public HyperGraph(SootMethod sootMethod, MethodDeclaration md) {
        super(sootMethod);
        cfg = new CFG(sootMethod);
    }

    public void plot(String fileDir) {
        GraphPrinter.plot(this, fileDir, "hyper.dot");
    }

    public void plotDDG(String fileDir) {
        GraphPrinter.plot(ddg, fileDir, "ddg.dot");
    }

    public void plotCDG(String fileDir) {
        GraphPrinter.plot(cdg, fileDir, "cdg.dot");
    }

    public void plotCFG(String fileDir) {
        GraphPrinter.plot(cfg, fileDir, "cfg.dot");
    }

    public CDG getCdg() {
        return cdg;
    }

    public CFG getCfg() {
        return cfg;
    }

    public ASTTree getAstg() {
        return astg;
    }

    public boolean isFailed() {
        return failed;
    }

    public DDG getDdg() {
        return ddg;
    }
}
