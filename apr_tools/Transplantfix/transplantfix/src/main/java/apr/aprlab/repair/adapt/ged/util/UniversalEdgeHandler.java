package apr.aprlab.repair.adapt.ged.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import apr.aprlab.repair.adapt.ged.Constants;

@SuppressWarnings({ "rawtypes" })
public class UniversalEdgeHandler implements IEdgeHandler {

    public void handleEdges(EditPath p, GraphComponent u_start, GraphComponent u_end) {
        if (u_end.getComponentId().equals(Constants.EPS_ID)) {
            LinkedList edges = ((Node) u_start).getEdges();
            Node v;
            for (int i = 0; i < edges.size(); i++) {
                Edge e = (Edge) edges.get(i);
                v = e.getOtherEnd((Node) u_start);
                if (!p.getUnUsedNodes1().contains(v)) {
                    p.addDistortion(e, Constants.EPS_COMPONENT);
                }
            }
        }
        if (u_start.getComponentId().equals(Constants.EPS_ID)) {
            LinkedList edges = ((Node) u_end).getEdges();
            Node v;
            for (int i = 0; i < edges.size(); i++) {
                Edge e = (Edge) edges.get(i);
                v = e.getOtherEnd((Node) u_end);
                if (!p.getUnUsedNodes2().contains(v)) {
                    p.addDistortion(Constants.EPS_COMPONENT, e);
                }
            }
        }
        if (!u_end.getComponentId().equals(Constants.EPS_ID) && !u_start.getComponentId().equals(Constants.EPS_ID)) {
            LinkedList edges = ((Node) u_start).getEdges();
            GraphComponent v_start;
            GraphComponent v_end;
            for (int i = 0; i < edges.size(); i++) {
                Edge e_start = (Edge) edges.get(i);
                v_start = e_start.getOtherEnd((Node) u_start);
                Enumeration enumeration = p.getDistortions().keys();
                int containsKey = 0;
                GraphComponent gc = new GraphComponent();
                while (enumeration.hasMoreElements()) {
                    GraphComponent key = (GraphComponent) enumeration.nextElement();
                    if (v_start.getComponentId().equals(key.getComponentId())) {
                        gc = (GraphComponent) p.getDistortions().get(key);
                        containsKey = 1;
                        break;
                    }
                }
                if (containsKey == 1) {
                    v_end = gc;
                    if (v_end.getComponentId().equals(Constants.EPS_ID)) {
                        p.addDistortion(e_start, Constants.EPS_COMPONENT);
                    } else {
                        Edge e_end;
                        e_end = getEdgeBetween((Node) u_end, v_end);
                        if (e_end != null) {
                            if (((Node) u_end).isDirected() == true) {
                                boolean gooddirection = AreEdgesTheSameDirection(e_start, e_end, (Node) u_start, (Node) v_start, (Node) u_end, (Node) v_end);
                                e_start.setInverted(!gooddirection);
                                e_end.setInverted(!gooddirection);
                            }
                            p.addDistortion(e_start, e_end);
                        } else {
                            p.addDistortion(e_start, Constants.EPS_COMPONENT);
                        }
                    }
                }
            }
            edges = ((Node) u_end).getEdges();
            Edge e_start;
            for (int i = 0; i < edges.size(); i++) {
                Edge e_end = (Edge) edges.get(i);
                v_end = e_end.getOtherEnd((Node) u_end);
                Enumeration enumeration = p.getDistortions().keys();
                GraphComponent key = new GraphComponent();
                GraphComponent value = new GraphComponent();
                int containsKey = 0;
                while (enumeration.hasMoreElements()) {
                    key = (GraphComponent) enumeration.nextElement();
                    value = (GraphComponent) p.getDistortions().get(key);
                    if (v_end.getComponentId().equals(value.getComponentId())) {
                        containsKey = 1;
                        break;
                    }
                }
                if (containsKey == 1) {
                    v_start = key;
                    e_start = getEdgeBetween((Node) u_start, v_start);
                    if (e_start == null) {
                        p.addDistortion(Constants.EPS_COMPONENT, e_end);
                    }
                }
            }
        }
    }

    private boolean AreEdgesTheSameDirection(Edge e_start, Edge e_end, GraphComponent u_start, Node v_start, GraphComponent u_end, Node v_end) {
        if (e_start != null && e_end != null) {
            if (e_start.getStartNode() == v_start && e_end.getStartNode() == v_end)
                return true;
        }
        return false;
    }

    private Edge getEdgeBetween(Node n1, GraphComponent n2) {
        Iterator iter = n1.getEdges().iterator();
        Node temp;
        while (iter.hasNext()) {
            Edge e = (Edge) iter.next();
            temp = e.getOtherEnd(n1);
            if (temp.getComponentId().equals(n2.getComponentId())) {
                return e;
            }
        }
        return null;
    }
}
