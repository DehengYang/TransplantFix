package apr.aprlab.utils.graph.ddg;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.toolkits.scalar.ValueUnitPair;
import java.util.ArrayList;
import java.util.List;
import apr.aprlab.utils.graph.ProgramNode;

public class DDGNode extends ProgramNode {

    private final ArrayList<Value> defs;

    private final ArrayList<Value> uses;

    DDGNode(Unit u) {
        super(u);
        defs = setDefs(u);
        uses = setUses(u);
    }

    public ArrayList<Value> getDefs() {
        return defs;
    }

    public ArrayList<Value> getUses() {
        return uses;
    }

    private ArrayList<Value> setDefs(Unit unit) {
        if (unit instanceof InvokeStmt) {
            InvokeExpr invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
            if (invokeExpr instanceof SpecialInvokeExpr) {
                ValueBox vb = new ValueUnitPair(handleSpecialInvoke((SpecialInvokeExpr) invokeExpr), unit);
                List<ValueBox> defBox = new ArrayList<>();
                defBox.add(vb);
                return getLocalAndInstanceFieldRef(defBox);
            }
        }
        return getLocalAndInstanceFieldRef(unit.getDefBoxes());
    }

    private Value handleSpecialInvoke(SpecialInvokeExpr expr) {
        return expr.getBase();
    }

    private ArrayList<Value> setUses(Unit unit) {
        return getLocalAndInstanceFieldRef(unit.getUseBoxes());
    }

    private ArrayList<Value> getLocalAndInstanceFieldRef(List<ValueBox> valueboxes) {
        ArrayList<Value> values = new ArrayList<>();
        for (ValueBox valuebox : valueboxes) {
            Value v = valuebox.getValue();
            if ((v instanceof Local) || (v instanceof InstanceFieldRef))
                if (!values.contains(v))
                    values.add(v);
        }
        return values;
    }
}
