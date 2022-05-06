package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.List;
import apr.aprlab.repair.snippet.MethodSnippet;

public abstract class GedAction {

    protected MethodSnippet srcMethodSnippet;

    protected PosRange posRange;

    protected String fileString;

    protected List<String> newStrings = new ArrayList<String>();

    public GedAction(MethodSnippet srcMethodSnippet) {
        this.srcMethodSnippet = srcMethodSnippet;
        fileString = srcMethodSnippet.getFileString();
    }

    public String getClassNameToBePatched() {
        return srcMethodSnippet.getClassName();
    }

    public MethodSnippet getSrcMethodSnippet() {
        return srcMethodSnippet;
    }

    public int getStartPos() {
        return posRange.getStartPos();
    }

    public int getEndPos() {
        return posRange.getEndPos();
    }

    public abstract PosRange getSrcPosRange();

    public abstract List<String> getNewStrings();

    public abstract List<DependentAction> setNewStrings();

    protected String getNewStrings(int combinedIndex) {
        if (combinedIndex < newStrings.size()) {
            return newStrings.get(combinedIndex);
        } else {
            return newStrings.get(0);
        }
    }
}
