package apr.aprlab.repair.adapt.ged.action;

import java.util.List;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.snippet.MethodSnippet;

public class ReversedCondAction extends GedAction {

    private Node condNode;

    public ReversedCondAction(MethodSnippet srcMethodSnippet, Node node) {
        super(srcMethodSnippet);
        this.condNode = node;
        this.posRange = new PosRange(node);
    }

    @Override
    public String toString() {
        String string = String.format("condNode to be reversed: %s", condNode.getComponentId());
        return string;
    }

    @Override
    public List<String> getNewStrings() {
        String newString = "!(" + condNode.getOldAttribute().getAstString() + ")";
        newStrings.add(newString);
        return newStrings;
    }

    @Override
    public PosRange getSrcPosRange() {
        return posRange;
    }

    @Override
    public List<DependentAction> setNewStrings() {
        return null;
    }
}
