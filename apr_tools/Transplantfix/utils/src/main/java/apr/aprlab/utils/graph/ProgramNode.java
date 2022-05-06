package apr.aprlab.utils.graph;

import soot.Unit;

public abstract class ProgramNode {

    private final Unit unit;

    private String id;

    public ProgramNode(Unit u) {
        unit = u;
    }

    public int getLineNumber() {
        return unit.getJavaSourceStartLineNumber();
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return String.format("line: %s\nunit: %s\n<%s>", getLineNumber(), unit.toString(), unit.getClass());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProgramNode))
            return false;
        return ((ProgramNode) o).getUnit().equals(unit);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
