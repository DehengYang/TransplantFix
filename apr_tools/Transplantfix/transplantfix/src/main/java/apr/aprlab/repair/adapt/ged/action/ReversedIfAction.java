package apr.aprlab.repair.adapt.ged.action;

import java.util.List;
import apr.aprlab.repair.adapt.ged.action.extract.TraversalUtil;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReversedIfAction extends GedAction {

    public static final Logger logger = LogManager.getLogger(ReversedIfAction.class);

    private Node condNode;

    private PosRange falseBranchPosRange;

    private PosRange trueBranchPosRange;

    private String fileString;

    public ReversedIfAction(MethodSnippet srcMethodSnippet, Node node) {
        super(srcMethodSnippet);
        this.condNode = node;
        fileString = srcMethodSnippet.getFileString();
        Node exitNode = TraversalUtil.getExitNode(node, false);
        List<Node> falseBranchNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, false, exitNode);
        List<Node> trueBranchNodes = TraversalUtil.depthFirstTraversalForNewOrOldForBranchStopAtNode(node, false, true, exitNode);
        TraversalUtil.excludeNodeAndItsAfter(node, falseBranchNodes);
        TraversalUtil.excludeNodeAndItsAfter(node, trueBranchNodes);
        if (!trueBranchNodes.isEmpty()) {
            falseBranchPosRange = new PosRange(trueBranchNodes, true);
        } else {
            logger.warn("trueBranchNodes is empty!");
            falseBranchPosRange = new PosRange(exitNode.getOldStartZeroPos(), exitNode.getOldStartZeroPos());
        }
        if (!falseBranchNodes.isEmpty()) {
            trueBranchPosRange = new PosRange(falseBranchNodes, true);
        } else {
            logger.warn("falseBranchNodes is empty!");
            trueBranchPosRange = new PosRange(exitNode.getOldStartZeroPos(), exitNode.getOldStartZeroPos());
        }
        posRange = new PosRange(falseBranchPosRange, trueBranchPosRange);
    }

    @Override
    public String toString() {
        String string = String.format("condNode to be reversed: %s", condNode.getComponentId());
        return string;
    }

    @Override
    public List<String> getNewStrings() {
        return newStrings;
    }

    @Override
    public PosRange getSrcPosRange() {
        return posRange;
    }

    public Node getCondNode() {
        return condNode;
    }

    public PosRange getFalseBranchPosRange() {
        return falseBranchPosRange;
    }

    public PosRange getTrueBranchPosRange() {
        return trueBranchPosRange;
    }

    public String getFileString() {
        return fileString;
    }

    @Override
    public List<DependentAction> setNewStrings() {
        if (falseBranchPosRange.getEndPos() > trueBranchPosRange.getStartPos()) {
            logger.error("the reversed if false branch is below the true branch: {} vs. {} for condNode: {}", falseBranchPosRange, trueBranchPosRange, condNode);
            return null;
        }
        String betweenReversedString = StringUtil.substring(fileString, falseBranchPosRange.getEndPos(), trueBranchPosRange.getStartPos());
        String falseBranchString = StringUtil.substring(fileString, falseBranchPosRange.getStartPos(), falseBranchPosRange.getEndPos());
        String trueBranchString = StringUtil.substring(fileString, trueBranchPosRange.getStartPos(), trueBranchPosRange.getEndPos());
        String newString = trueBranchString + betweenReversedString + falseBranchString;
        newStrings.add(newString);
        return null;
    }
}
