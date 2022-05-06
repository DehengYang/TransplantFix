package apr.aprlab.utils.graph.printer;

import java.io.IOException;
import java.nio.file.Paths;
import apr.aprlab.utils.general.CmdUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.graph.ProgramEdge;
import apr.aprlab.utils.graph.ProgramGraph;
import apr.aprlab.utils.graph.ProgramNode;
import apr.aprlab.utils.graph.cdg.CDGEdge;
import apr.aprlab.utils.graph.ddg.DDGEdge;
import apr.aprlab.utils.tree.ASTTree;

public class GraphPrinter {

    public static void plot(ProgramGraph graph, String fileDir, String fileName) {
        StringBuilder sb = new StringBuilder();
        ExceptionUtil.assertFileExists(fileDir);
        String filePath = Paths.get(fileDir, fileName).toString();
        sb.append("digraph \"graph\" {\n");
        sb.append("    label=\"graph\";\n");
        sb.append("node [shape=box];\n");
        for (ProgramNode node : graph.getNodes()) {
            plotNode(node, sb);
            sb.append("\n");
        }
        for (ProgramEdge edge : graph.getEdges()) {
            plotEdge(edge, sb);
            sb.append("\n");
        }
        sb.append("}");
        FileUtil.writeToFile(filePath, sb.toString(), false);
        String savePngPath = Paths.get(fileDir, fileName + ".png").toString();
        String cmd = String.format("dot -Tpng %s -o %s", filePath, savePngPath);
        CmdUtil.runCmd(cmd);
    }

    private static void plotNode(ProgramNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("END");
            return;
        }
        String nodeID = "\"";
        nodeID += node.getUnit().getJavaSourceStartLineNumber() + ": ";
        nodeID += node.getUnit().toString().replace("\"", "");
        nodeID += "\"";
        sb.append(nodeID);
    }

    private static void plotEdge(ProgramEdge edge, StringBuilder sb) {
        ProgramNode from = edge.getSrc();
        ProgramNode to = edge.getTgt();
        plotNode(from, sb);
        sb.append("->");
        plotNode(to, sb);
        if (edge instanceof DDGEdge) {
            sb.append("[label=\"" + ((DDGEdge) edge).getValue().toString() + "\", style=\"dotted\"];\n");
        } else if (edge instanceof CDGEdge) {
            sb.append("[color=\"red\", style=\"dotted\"];\n");
        }
    }

    public static void plot(ASTTree astg, String fileDir, String fileName) {
        StringBuilder sb = new StringBuilder();
        ExceptionUtil.assertFileExists(fileDir);
        String filePath = Paths.get(fileDir, fileName).toString();
        sb.append("digraph \"graph\" {\n");
        sb.append("    label=\"graph\";\n");
        sb.append("node [shape=box];\n");
        getNodeEdgeStr(astg, sb);
        sb.append("}");
        FileUtil.writeToFile(filePath, sb.toString(), false);
        String savePngPath = Paths.get(fileDir, fileName + ".png").toString();
        String cmd = String.format("dot -Tpng %s -o %s", filePath, savePngPath);
        CmdUtil.runCmd(cmd);
    }

    private static void getNodeEdgeStr(ASTTree astg, StringBuilder sb) {
        sb.append(String.format("\"%s\"\n", astg.toGraphString()));
        for (ASTTree child : astg.getChildren()) {
            sb.append(String.format("\"%s\" -> \"%s\"\n", astg.toGraphString(), child.toGraphString()));
            getNodeEdgeStr(child, sb);
        }
    }
}
