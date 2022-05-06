package apr.aprlab.utils.graph;

import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.toolkits.pointer.FullObjectSet;

public class AliasQuery {

    public static boolean isAlias(Value value1, Value value2) {
        if (value1.equals(value2))
            return true;
        if (value1.toString().equals("this") && value2.toString().equals("this"))
            return true;
        PointsToSetInternal pts1 = getPointsToSet(value1);
        if (pts1 == null || pts1.isEmpty())
            return false;
        PointsToSetInternal pts2 = getPointsToSet(value2);
        if (pts2 == null || pts2.isEmpty())
            return false;
        return pts1.hasNonEmptyIntersection(pts2);
    }

    private static PointsToSetInternal getPointsToSet(Value value) {
        PointsToSetInternal pts = null;
        PointsToAnalysis ptsProvider = Scene.v().getPointsToAnalysis();
        if (value instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef) value;
            pts = (PointsToSetInternal) ptsProvider.reachingObjects((Local) ifr.getBase(), ifr.getField());
        } else if (value instanceof ArrayRef) {
            ArrayRef arrayRef = (ArrayRef) value;
            pts = (PointsToSetInternal) ptsProvider.reachingObjectsOfArrayElement(ptsProvider.reachingObjects((Local) arrayRef.getBase()));
        } else if (value instanceof Local) {
            PointsToSet tmp = ptsProvider.reachingObjects((Local) value);
            if (!(tmp instanceof FullObjectSet)) {
                pts = (PointsToSetInternal) ptsProvider.reachingObjects((Local) value);
            }
        } else if (value instanceof StaticFieldRef) {
            SootField field = ((StaticFieldRef) value).getField();
            pts = (PointsToSetInternal) ptsProvider.reachingObjects(field);
        }
        return pts;
    }
}
