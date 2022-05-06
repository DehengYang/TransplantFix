package apr.aprlab.repair.adapt.ged.action;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchSpace {

    public static final Logger logger = LogManager.getLogger(SearchSpace.class);

    List<GedAction> allActions = new ArrayList<GedAction>();

    List<DependentAction> dependentActions = new ArrayList<DependentAction>();

    public SearchSpace(List<GedAction> actions) {
        this.allActions = actions;
        dependentActions = constructSearchSpace(actions);
    }

    private List<DependentAction> constructSearchSpace(List<GedAction> actions) {
        List<DependentAction> depActions = new ArrayList<DependentAction>();
        List<DependentAction> oriDepActions = getOriDepActions(actions);
        depActions.addAll(oriDepActions);
        actions.removeAll(oriDepActions);
        int cnt = 0;
        for (GedAction action : actions) {
            if (cnt == 6) {
                logger.debug("");
            }
            logger.debug("depAction cnt: {}", cnt);
            DependentAction depAction = new DependentAction(action, allActions);
            depActions.add(depAction);
            if (cnt == 3) {
                logger.debug("");
            }
            logger.debug("extend to block dep action cnt: {}", cnt++);
            DependentAction extendToBlockDepAction = new DependentAction(action, allActions, false, true);
            if (depAction.isMeaningful()) {
                depActions.add(extendToBlockDepAction);
            }
        }
        for (GedAction curAction : actions) {
            if (curAction instanceof ReversedIfAction) {
                DependentAction depAction = new DependentAction((ReversedIfAction) curAction, actions, true, false);
                if (depAction.isMeaningful()) {
                    depActions.add(depAction);
                }
                cnt++;
            }
        }
        return depActions;
    }

    private List<DependentAction> getOriDepActions(List<GedAction> actions) {
        List<DependentAction> dependentActions = new ArrayList<DependentAction>();
        for (GedAction gedAction : actions) {
            if (gedAction instanceof DependentAction) {
                dependentActions.add((DependentAction) gedAction);
            }
        }
        return dependentActions;
    }

    public List<DependentAction> getDependentActions() {
        return dependentActions;
    }
}
