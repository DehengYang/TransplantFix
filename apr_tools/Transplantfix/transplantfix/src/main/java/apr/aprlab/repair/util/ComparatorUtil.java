package apr.aprlab.repair.util;

import java.util.Comparator;
import org.eclipse.jdt.core.dom.ASTNode;
import apr.aprlab.repair.adapt.ged.action.GedAction;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.fl.SuspiciousLocation;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.Pair;

public class ComparatorUtil {

    public static final Comparator<Node> nodeOldIdComparator = new Comparator<Node>() {

        @Override
        public int compare(Node o1, Node o2) {
            return Integer.compare(o1.getComponentIdInt(false), o2.getComponentIdInt(false));
        }
    };

    public static final Comparator<Node> lineNoCompForNew = new Comparator<Node>() {

        @Override
        public int compare(Node o1, Node o2) {
            return Integer.compare(o1.getAttribute().getLineNo(), o2.getAttribute().getLineNo());
        }
    };

    public static final Comparator<Node> lineNoCompForOld = new Comparator<Node>() {

        @Override
        public int compare(Node o1, Node o2) {
            return Integer.compare(o1.getOldAttribute().getLineNo(), o2.getOldAttribute().getLineNo());
        }
    };

    public static Comparator<MethodSnippet> methodSnippetComparator = new Comparator<MethodSnippet>() {

        @Override
        public int compare(MethodSnippet o1, MethodSnippet o2) {
            return Integer.compare(o1.getRange().getStartLineNo(), o2.getRange().getStartLineNo());
        }
    };

    public static Comparator<ASTNode> astNodeRangeCompator = new Comparator<ASTNode>() {

        @Override
        public int compare(ASTNode o1, ASTNode o2) {
            return RangeUtil.rangeContains(o1, o2);
        }
    };

    public static Comparator<Pair<ASTNode, String>> astNodePairComparator = new Comparator<Pair<ASTNode, String>>() {

        @Override
        public int compare(Pair<ASTNode, String> o1, Pair<ASTNode, String> o2) {
            return Integer.compare(o1.getLeft().getStartPosition(), o2.getLeft().getStartPosition());
        }
    };

    public static Comparator<GedAction> actionComparator = new Comparator<GedAction>() {

        @Override
        public int compare(GedAction o1, GedAction o2) {
            if (o1.getStartPos() != o2.getStartPos()) {
                return Integer.compare(o1.getStartPos(), o2.getStartPos());
            } else {
                return Integer.compare(o1.getEndPos(), o2.getEndPos());
            }
        }
    };

    public static Comparator<SuspiciousLocation> suspComparator = new Comparator<SuspiciousLocation>() {

        @Override
        public int compare(SuspiciousLocation o1, SuspiciousLocation o2) {
            return Double.compare(o2.getSuspValue(), o1.getSuspValue());
        }
    };
}
