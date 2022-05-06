package apr.aprlab.repair.adapt.ged.action;

import java.util.List;
import apr.aprlab.repair.snippet.MethodSnippet;

public class EndBraceDelAction extends GedAction {

    public EndBraceDelAction(MethodSnippet srcMethodSnippet, PosRange posRange) {
        super(srcMethodSnippet);
        this.posRange = posRange;
        setNewStrings();
    }

    @Override
    public List<DependentAction> setNewStrings() {
        newStrings.add("");
        return null;
    }

    @Override
    public PosRange getSrcPosRange() {
        return posRange;
    }

    @Override
    public String toString() {
        return posRange.toString();
    }

    @Override
    public List<String> getNewStrings() {
        return newStrings;
    }
}
