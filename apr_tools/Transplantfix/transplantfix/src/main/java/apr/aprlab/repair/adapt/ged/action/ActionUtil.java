package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Statement;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.utils.ast.ASTUtil;

public class ActionUtil {

    public static final Logger logger = LogManager.getLogger(ActionUtil.class);

    public static boolean containsEndBraceDelAction(List<GedAction> actions) {
        for (GedAction action : actions) {
            if (action instanceof EndBraceDelAction) {
                return true;
            }
        }
        return false;
    }

    public static void mergeEndBraceDelAction(List<GedAction> actions) {
        List<EndBraceDelAction> endBraceDelActions = getEndBraceDelActions(actions);
        actions.removeAll(endBraceDelActions);
        List<GedAction> newActions = new ArrayList<GedAction>(actions);
        for (EndBraceDelAction endBraceDelAction : endBraceDelActions) {
            PosRange posRange = endBraceDelAction.getSrcPosRange();
            boolean contains = false;
            for (GedAction gedAction : actions) {
                PosRange gedPosRange = gedAction.getSrcPosRange();
                if (gedPosRange.contains(posRange)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                newActions.add(endBraceDelAction);
            } else {
                logger.debug("Exclude the endBraceDelAction as it is already contained by other actions: {}", endBraceDelAction);
            }
        }
        actions.clear();
        actions.addAll(newActions);
    }

    private static List<EndBraceDelAction> getEndBraceDelActions(List<GedAction> actions) {
        List<EndBraceDelAction> endBraceDelActions = new ArrayList<EndBraceDelAction>();
        for (GedAction action : actions) {
            if (action instanceof EndBraceDelAction) {
                endBraceDelActions.add((EndBraceDelAction) action);
            }
        }
        return endBraceDelActions;
    }

    public static List<Statement> getStmtsFromDelNodes(List<Node> delNodes) {
        List<Statement> stmts = new ArrayList<Statement>();
        for (Node node : delNodes) {
            ASTNode astNode = node.getOldAttribute().getAstNode();
            if (astNode == null) {
                logger.warn("node has no astNode: {}", node);
                continue;
            }
            if (!(astNode instanceof Statement)) {
                Statement parentStmt = ASTUtil.getParentStmt(astNode);
                if (parentStmt != null) {
                    stmts.add(parentStmt);
                }
                logger.warn("astNode is not stmt: {}, its parent stmt is: {}", astNode, parentStmt);
            } else {
                stmts.add((Statement) astNode);
            }
        }
        return stmts;
    }
}
