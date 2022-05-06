package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.List;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class DependentActionUnUsed extends GedAction {

    public static final Logger logger = LogManager.getLogger(GedAction.class);

    private GedAction initAction;

    private List<GedAction> actions = new ArrayList<GedAction>();

    private ReversedIfAction parentIfAction;

    private UpdAction parentCondAction;

    private String fileString;

    public DependentActionUnUsed(GedAction action, List<GedAction> allActions) {
        this(action, allActions, true);
    }

    public DependentActionUnUsed(GedAction action, List<GedAction> allActions, boolean extendReversedIfAction) {
        super(action.getSrcMethodSnippet());
        this.initAction = action;
        this.fileString = srcMethodSnippet.getFileString();
        if (action instanceof ReversedIfAction) {
            ReversedIfAction reversedIfAction = (ReversedIfAction) action;
            parentIfAction = reversedIfAction;
            posRange = new PosRange(reversedIfAction.getSrcPosRange());
            if (extendReversedIfAction) {
                for (GedAction curAction : allActions) {
                    if (curAction == reversedIfAction) {
                        continue;
                    }
                    addCondUpdateAction(curAction, reversedIfAction, posRange, parentCondAction);
                    allContainedAction(curAction, reversedIfAction, actions);
                }
            }
        } else {
        }
    }

    private void allContainedAction(GedAction curAction, ReversedIfAction reversedIfAction, List<GedAction> actions) {
        if (reversedIfAction.getSrcPosRange().contains(curAction.getSrcPosRange())) {
            if (curAction instanceof ReversedIfAction) {
                ExceptionUtil.todo();
            }
            actions.add(curAction);
        }
    }

    private void addCondUpdateAction(GedAction curAction, ReversedIfAction reversedIfAction, PosRange posRange, UpdAction parentCondAction) {
        if (curAction instanceof UpdAction) {
            UpdAction updAction = (UpdAction) curAction;
            if (updAction.isUpdIfCond()) {
                Node condNode = updAction.getStartNode();
                if (reversedIfAction.getCondNode() == condNode) {
                    parentCondAction = updAction;
                    posRange = new PosRange(posRange, parentCondAction.getSrcPosRange());
                }
            }
        }
    }

    @Override
    public String toString() {
        String string = String.format("[%s] actions: %sparentIfAction: %s, parentCondAction: %s", posRange, PrintUtil.listToString(actions), parentIfAction, parentCondAction);
        return string;
    }

    public List<GedAction> getActions() {
        return actions;
    }

    public GedAction getInitAction() {
        return initAction;
    }

    public String getNewString() {
        String newString = "";
        int cutStartPos = getStartPos();
        if (parentCondAction != null) {
            newString += StringUtil.substring(fileString, cutStartPos, parentCondAction.getStartPos());
            int condEndPos = parentCondAction.getEndPos();
            cutStartPos = condEndPos;
        }
        if (parentIfAction != null) {
            newString += StringUtil.substring(fileString, cutStartPos, parentIfAction.getStartPos());
            String betweenReversedString = StringUtil.substring(fileString, parentIfAction.getFalseBranchPosRange().getEndPos(), parentIfAction.getTrueBranchPosRange().getStartPos());
            String falseBranchString = getActionsString(parentIfAction.getFalseBranchPosRange(), actions);
            String trueBranchString = getActionsString(parentIfAction.getTrueBranchPosRange(), actions);
            newString += trueBranchString + betweenReversedString + falseBranchString;
            cutStartPos = parentIfAction.getTrueBranchPosRange().getEndPos();
        } else {
            newString += getActionsString(posRange, actions);
            cutStartPos = getEndPos();
        }
        return newString;
    }

    private String getActionsString(PosRange posRange, List<GedAction> actions) {
        String newString = "";
        int startPos = posRange.getStartPos();
        int endPos = posRange.getEndPos();
        int cutStartPos = startPos;
        for (GedAction action : actions) {
            if (posRange.containsOrEqual(action.getSrcPosRange())) {
                newString += StringUtil.substring(fileString, cutStartPos, action.getStartPos());
                cutStartPos = action.getEndPos();
            }
        }
        newString += StringUtil.substring(fileString, cutStartPos, endPos);
        return newString;
    }

    @Override
    public PosRange getSrcPosRange() {
        return posRange;
    }
}
