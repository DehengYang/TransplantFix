package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;

public class DependentAction extends GedAction {

    public static final Logger logger = LogManager.getLogger(GedAction.class);

    private GedAction initAction;

    private List<GedAction> actions = new ArrayList<GedAction>();

    private ReversedIfAction parentIfAction;

    private UpdAction parentCondAction;

    private int maxPatternCnt;

    private int curPatternIndex = 0;

    private String originalString;

    private boolean extendToBlock = false;

    private ASTNode thisIfOrLoopAstNode;

    private boolean reallyExtend = false;

    public DependentAction(GedAction action, List<GedAction> allActions) {
        this(action, allActions, false, false);
    }

    public DependentAction(GedAction action, List<GedAction> allActions, boolean extendReversedIfAction, boolean extendToBlock) {
        super(action.getSrcMethodSnippet());
        this.initAction = action;
        this.extendToBlock = extendToBlock;
        if (action instanceof ReversedIfAction) {
            ReversedIfAction reversedIfAction = (ReversedIfAction) action;
            parentIfAction = reversedIfAction;
            posRange = new PosRange(reversedIfAction.getSrcPosRange());
            if (extendReversedIfAction) {
                addCondUpdateAction(allActions, reversedIfAction);
                allContainedAction(allActions, reversedIfAction, actions);
            }
        } else {
            actions.add(action);
            posRange = new PosRange(action.getSrcPosRange());
            addEndBraceDelActions(action, actions, posRange);
            if (extendToBlock) {
                reallyExtend = extendToBlock(action, allActions, actions, posRange);
            }
        }
        if (ActionUtil.containsEndBraceDelAction(actions)) {
            ActionUtil.mergeEndBraceDelAction(actions);
        }
        Collections.sort(actions, ComparatorUtil.actionComparator);
        if (actions.size() > 1) {
            if (hasConflictsPosRanges()) {
                logger.error("current actions posRanges conflict! so we discard it!");
                actions.clear();
                return;
            }
        }
        if (extendToBlock && reallyExtend) {
            posRange.update(new PosRange(thisIfOrLoopAstNode));
            extendNewStrings();
        }
        originalString = obtainOriginalString(posRange);
        maxPatternCnt = getPatternCnt();
        setNewStrings();
        if (extendToBlock && reallyExtend) {
            extendNewStrings();
        }
    }

    private String obtainOriginalString(PosRange posRange) {
        return StringUtil.substring(fileString, posRange.getStartPos(), posRange.getEndPos());
    }

    private boolean hasConflictsPosRanges() {
        for (int i = 0; i < actions.size() - 1; i++) {
            PosRange thisPosRange = actions.get(i).getSrcPosRange();
            for (int j = i + 1; j < actions.size(); j++) {
                PosRange otherPosRange = actions.get(j).getSrcPosRange();
                if (thisPosRange.hasIntersection(otherPosRange)) {
                    logger.error("the two actions' posRanges conflict: {} --- {}", actions.get(i), actions.get(j));
                    return true;
                }
            }
        }
        return false;
    }

    private void extendNewStrings() {
        ExceptionUtil.myAssert(extendToBlock && reallyExtend);
        for (String newString : new ArrayList<>(newStrings)) {
            String newStrWithoutDelOriStr = newString + originalString;
            newStrings.add(newStrWithoutDelOriStr);
        }
    }

    public DependentAction(List<UpdAction> updActions) {
        super(updActions.get(0).getSrcMethodSnippet());
        actions.addAll(updActions);
        Collections.sort(actions, ComparatorUtil.actionComparator);
        initAction = actions.get(0);
        posRange = new PosRange(initAction.getSrcPosRange());
        for (UpdAction updAction : updActions) {
            posRange.update(updAction.getSrcPosRange());
        }
        maxPatternCnt = 1;
        ExceptionUtil.myAssert(!ActionUtil.containsEndBraceDelAction(actions));
        setNewStrings();
        originalString = StringUtil.substring(fileString, posRange.getStartPos(), posRange.getEndPos());
    }

    private boolean extendToBlock(GedAction action, List<GedAction> allActions, List<GedAction> actions2, PosRange posRange) {
        if (!(action instanceof UpdAction)) {
            return false;
        }
        boolean reallyExtend = false;
        UpdAction updAction = (UpdAction) action;
        thisIfOrLoopAstNode = updAction.getIfOrLoopAstNode();
        if (thisIfOrLoopAstNode != null) {
            for (GedAction gedAction : allActions) {
                if (gedAction == updAction) {
                    continue;
                }
                if (gedAction instanceof UpdAction) {
                    UpdAction curUpdAction = (UpdAction) gedAction;
                    ASTNode ifOrLoopAstNode = curUpdAction.getIfOrLoopAstNode();
                    if (ifOrLoopAstNode != null && ifOrLoopAstNode == thisIfOrLoopAstNode) {
                        actions.add(gedAction);
                        addEndBraceDelActions(gedAction, actions, posRange);
                        posRange.update(gedAction.getSrcPosRange());
                        reallyExtend = true;
                    }
                }
            }
        }
        return reallyExtend;
    }

    private void addEndBraceDelActions(GedAction action, List<GedAction> actions, PosRange posRange) {
        if (action instanceof UpdAction) {
            UpdAction updAction = (UpdAction) action;
            List<EndBraceDelAction> endBraceDelActions = updAction.getEndBraceDelActions();
            for (GedAction braceAction : endBraceDelActions) {
                if (posRange.contains(braceAction.getSrcPosRange())) {
                } else {
                    posRange.update(braceAction.getSrcPosRange());
                    actions.add(braceAction);
                }
            }
        }
    }

    private void allContainedAction(List<GedAction> allActions, ReversedIfAction reversedIfAction, List<GedAction> actions) {
        for (GedAction curAction : allActions) {
            if (curAction == reversedIfAction) {
                continue;
            }
            if (reversedIfAction.getSrcPosRange().contains(curAction.getSrcPosRange())) {
                actions.add(curAction);
                for (EndBraceDelAction endBraceDelAction : getEndBraceDelActions(curAction)) {
                    actions.add(endBraceDelAction);
                    if (!posRange.contains(endBraceDelAction.getSrcPosRange())) {
                        logger.error("posRange does not cover endBraceDelAction: {} --- {}", posRange, endBraceDelAction);
                        posRange.update(endBraceDelAction.getSrcPosRange());
                    }
                }
            }
        }
    }

    private List<EndBraceDelAction> getEndBraceDelActions(GedAction curAction) {
        List<EndBraceDelAction> endBraceDelActions = new ArrayList<>();
        if (curAction instanceof UpdAction) {
            UpdAction updAction = (UpdAction) curAction;
            endBraceDelActions = updAction.getEndBraceDelActions();
        }
        return endBraceDelActions;
    }

    private void addCondUpdateAction(List<GedAction> allActions, ReversedIfAction reversedIfAction) {
        for (GedAction curAction : allActions) {
            if (curAction == reversedIfAction) {
                continue;
            }
            if (curAction instanceof UpdAction) {
                UpdAction updAction = (UpdAction) curAction;
                if (updAction.isUpdIfCond()) {
                    Node condNode = updAction.getStartNode();
                    if (reversedIfAction.getCondNode() == condNode) {
                        parentCondAction = updAction;
                        posRange.update(parentCondAction.getSrcPosRange());
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        String string = String.format("[%s] actions: %sparentIfAction: %s, parentCondAction: %s", posRange, PrintUtil.listToString(actions).trim(), parentIfAction, parentCondAction);
        return string;
    }

    public List<GedAction> getActions() {
        return actions;
    }

    public GedAction getInitAction() {
        return initAction;
    }

    @Override
    public List<DependentAction> setNewStrings() {
        for (int i = 0; i < maxPatternCnt; i++) {
            String newString = getNewString(actions, i);
            newStrings.add(newString);
        }
        return null;
    }

    private int getPatternCnt() {
        int maxCombinedCnt = 1;
        for (GedAction action : actions) {
            maxCombinedCnt = Math.max(maxCombinedCnt, action.getNewStrings().size());
        }
        return maxCombinedCnt;
    }

    public String getNewString(List<GedAction> selectedActions, int combinedIndex) {
        String newString = "";
        int cutStartPos = getStartPos();
        if (parentCondAction != null) {
            newString += StringUtil.substring(fileString, cutStartPos, parentCondAction.getStartPos());
            int condEndPos = parentCondAction.getEndPos();
            newString += parentCondAction.getNewStrings().get(0);
            cutStartPos = condEndPos;
        }
        if (parentIfAction != null) {
            newString += StringUtil.substring(fileString, cutStartPos, parentIfAction.getStartPos());
            String betweenReversedString = StringUtil.substring(fileString, parentIfAction.getFalseBranchPosRange().getEndPos(), parentIfAction.getTrueBranchPosRange().getStartPos());
            String falseBranchString = getActionsString(parentIfAction.getFalseBranchPosRange(), selectedActions, combinedIndex);
            String trueBranchString = getActionsString(parentIfAction.getTrueBranchPosRange(), selectedActions, combinedIndex);
            newString += trueBranchString + betweenReversedString + falseBranchString;
            cutStartPos = parentIfAction.getTrueBranchPosRange().getEndPos();
        } else {
            newString += getActionsString(posRange, selectedActions, combinedIndex);
            cutStartPos = getEndPos();
        }
        return newString;
    }

    private String getActionsString(PosRange posRange, List<GedAction> actions, int combinedIndex) {
        String newString = "";
        int startPos = posRange.getStartPos();
        int endPos = posRange.getEndPos();
        int cutStartPos = startPos;
        List<GedAction> newActions = new ArrayList<GedAction>(actions);
        List<EndBraceDelAction> realEndBraceDelActions = new ArrayList<EndBraceDelAction>();
        for (GedAction action : new ArrayList<>(actions)) {
            if (action instanceof UpdAction) {
                Map<String, List<EndBraceDelAction>> delStringEndBraceMap = ((UpdAction) action).getDelStringEndBraceMap();
                String curString = action.getNewStrings(combinedIndex);
                if (delStringEndBraceMap.containsKey(curString)) {
                    for (EndBraceDelAction endBraceDelAction : delStringEndBraceMap.get(curString)) {
                        if (!realEndBraceDelActions.contains(endBraceDelAction)) {
                            realEndBraceDelActions.add(endBraceDelAction);
                        }
                    }
                }
            }
        }
        for (GedAction action : newActions) {
            if (action instanceof EndBraceDelAction) {
                if (!realEndBraceDelActions.contains(action)) {
                    logger.info("exclude current end brace del action: {}", action);
                    continue;
                }
            }
            if (posRange.containsOrEqual(action.getSrcPosRange())) {
                if (cutStartPos > action.getStartPos()) {
                    logger.debug("");
                }
                newString += StringUtil.substring(fileString, cutStartPos, action.getStartPos());
                newString += action.getNewStrings(combinedIndex);
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

    @Override
    public List<String> getNewStrings() {
        return newStrings;
    }

    public ReversedIfAction getParentIfAction() {
        return parentIfAction;
    }

    public UpdAction getParentCondAction() {
        return parentCondAction;
    }

    public int getMaxPatternCnt() {
        return maxPatternCnt;
    }

    public void setPatternIndex(int patternIndex) {
        this.curPatternIndex = patternIndex;
    }

    public String getCurPatternNewString() {
        String string = getNewStrings().get(curPatternIndex);
        return string;
    }

    public String getOriginalString() {
        return originalString;
    }

    public String getFilePath() {
        return srcMethodSnippet.getFilePath();
    }

    public boolean isMeaningful() {
        return !actions.isEmpty();
    }

    public int getCurPatternIndex() {
        return curPatternIndex;
    }
}
