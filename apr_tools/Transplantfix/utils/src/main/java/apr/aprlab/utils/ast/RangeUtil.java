package apr.aprlab.utils.ast;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import apr.aprlab.utils.general.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RangeUtil {

    public static final Logger logger = LogManager.getLogger(RangeUtil.class);

    public static boolean lineInRange(Range nodeRange, int lineNo) {
        if (lineNo >= nodeRange.getStartLineNo() && lineNo <= nodeRange.getEndLineNo()) {
            return true;
        } else {
            return false;
        }
    }

    public static int rangeContains(int startLarge, int endLarge, int start, int end) {
        if (startLarge <= start && endLarge >= end) {
            if (startLarge == start && endLarge == end) {
                return 0;
            }
            return 1;
        }
        return -1;
    }

    public static int rangeContains(Range largeRange, Range range) {
        int startLarge = largeRange.getStartLineNo();
        int endLarge = largeRange.getEndLineNo();
        int start = range.getStartLineNo();
        int end = range.getEndLineNo();
        return rangeContains(startLarge, endLarge, start, end);
    }

    public static int rangeContains(ASTNode largerNode, ASTNode node) {
        return rangeContains(getRange(largerNode), getRange(node));
    }

    public static int rangeContains(ASTNode largerNode, Range range) {
        return rangeContains(getRange(largerNode), range);
    }

    public static int getStartLineNo(CompilationUnit cu, ASTNode node) {
        if (node == null) {
            logger.debug("");
        }
        int startLineNum = cu.getLineNumber(node.getStartPosition());
        return startLineNum;
    }

    public static int getEndLineNo(CompilationUnit cu, ASTNode node) {
        int endLineNum = cu.getLineNumber(node.getStartPosition() + node.getLength());
        return endLineNum;
    }

    public static int getStartColumnNo(CompilationUnit cu, ASTNode node) {
        int startColNum = cu.getColumnNumber(node.getStartPosition());
        return startColNum;
    }

    public static Range getRange(CompilationUnit cu, ASTNode node) {
        return new Range(getStartLineNo(cu, node), getEndLineNo(cu, node));
    }

    public static Range getRange(ASTNode node) {
        CompilationUnit cu = ASTUtil.getCompilationUnit(node);
        return new Range(getStartLineNo(cu, node), getEndLineNo(cu, node));
    }

    public static boolean containsDstNode(ASTNode src, ASTNode dst) {
        boolean is = src.getStartPosition() <= dst.getStartPosition() && (src.getStartPosition() + src.getLength() >= dst.getStartPosition() + dst.getLength());
        return is;
    }

    public static boolean fullyContainsDstNode(ASTNode src, ASTNode dst) {
        boolean is = containsDstNode(src, dst) && src.getLength() != dst.getLength();
        return is;
    }

    public static Pair<Integer, Integer> getPosRange(CompilationUnit compilationUnit, ASTNode astNode) {
        Range lineRange = getRange(compilationUnit, astNode);
        int startPos = compilationUnit.getPosition(lineRange.getStartLineNo(), 0);
        int endPos = compilationUnit.getPosition(lineRange.getEndLineNo() + 1, 0) - 1;
        return new Pair<>(startPos, endPos);
    }

    public static boolean lineInRange(ASTNode astNode, int lineNo) {
        Range range = getRange(astNode);
        boolean is = lineInRange(range, lineNo);
        return is;
    }

    public static boolean isBeforeDstNode(ASTNode src, ASTNode dst) {
        boolean is = src.getStartPosition() + src.getLength() < dst.getStartPosition();
        return is;
    }
}
