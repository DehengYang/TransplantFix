package apr.aprlab.repair.adapt.ged.action;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTNode;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PosRange {

    public static final Logger logger = LogManager.getLogger(PosRange.class);

    private int startPos;

    private int endPos;

    public PosRange(Node startNode, Node endNode, boolean include) {
        ExceptionUtil.assertFalse(startNode == endNode);
        setStartPos(startNode, include);
        setEndPos(endNode, include);
    }

    private void setEndPos(Node endNode, boolean include) {
        if (include) {
            endPos = endNode.getOldEndPosForAction();
        } else {
            endPos = endNode.getOldStartPosForAction();
        }
    }

    private void setStartPos(Node startNode, boolean include) {
        if (include) {
            startPos = startNode.getOldStartPosForAction();
        } else {
            startPos = startNode.getOldEndPosForAction();
        }
    }

    public PosRange(List<Node> delNodes, boolean include) {
        ExceptionUtil.assertFalse(delNodes.isEmpty());
        if (delNodes.size() == 1) {
            if (include) {
                startPos = delNodes.get(0).getOldStartPosForAction();
                endPos = delNodes.get(0).getOldEndPosForAction();
            } else {
                ExceptionUtil.todo();
            }
        } else {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (Node node : delNodes) {
                if (node.getOldAttribute().getAstNode() == null) {
                    logger.warn("node has no astNode: {}", node);
                    continue;
                }
                min = Math.min(node.getOldStartPosForAction(), min);
                max = Math.max(node.getOldEndPosForAction(), max);
            }
            startPos = min;
            endPos = max;
        }
    }

    public PosRange(Node startNode) {
        startPos = startNode.getOldStartPosForAction();
        endPos = startNode.getOldEndPosForAction();
    }

    public PosRange(int startPos, int endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public PosRange(PosRange srcPosRange, PosRange dstPosRange) {
        startPos = Math.min(srcPosRange.getStartPos(), dstPosRange.getStartPos());
        endPos = Math.max(srcPosRange.getEndPos(), dstPosRange.getEndPos());
    }

    public void update(PosRange dstPosRange) {
        startPos = Math.min(startPos, dstPosRange.getStartPos());
        endPos = Math.max(endPos, dstPosRange.getEndPos());
    }

    public PosRange(PosRange srcPosRange) {
        startPos = srcPosRange.getStartPos();
        endPos = srcPosRange.getEndPos();
    }

    public PosRange(ASTNode astNode) {
        startPos = astNode.getStartPosition();
        endPos = startPos + astNode.getLength();
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    @Override
    public String toString() {
        return String.format("%s, %s", startPos, endPos);
    }

    public boolean contains(PosRange srcPosRange) {
        int is = RangeUtil.rangeContains(this.getStartPos(), this.getEndPos(), srcPosRange.getStartPos(), srcPosRange.getEndPos());
        if (is >= 0) {
            return true;
        }
        return false;
    }

    public boolean isEqual(PosRange srcPosRange) {
        boolean is = startPos == srcPosRange.getStartPos() && endPos == srcPosRange.getEndPos();
        return is;
    }

    public boolean containsOrEqual(PosRange srcPosRange) {
        int is = RangeUtil.rangeContains(this.getStartPos(), this.getEndPos(), srcPosRange.getStartPos(), srcPosRange.getEndPos());
        if (is >= 0) {
            return true;
        }
        return false;
    }

    public boolean hasIntersection(PosRange srcPosRange) {
        if (startPos >= srcPosRange.getEndPos() || endPos <= srcPosRange.getStartPos()) {
            return false;
        } else {
            return true;
        }
    }
}
