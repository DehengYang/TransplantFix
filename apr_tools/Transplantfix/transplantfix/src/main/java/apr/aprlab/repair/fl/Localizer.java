package apr.aprlab.repair.fl;

import java.util.ArrayList;
import java.util.List;

public abstract class Localizer {

    protected List<SuspiciousMethod> suspiciousMethods = new ArrayList<SuspiciousMethod>();

    public List<SuspiciousMethod> getSuspiciousMethods() {
        return suspiciousMethods;
    }

    public abstract List<SuspiciousStmt> localize();
}
