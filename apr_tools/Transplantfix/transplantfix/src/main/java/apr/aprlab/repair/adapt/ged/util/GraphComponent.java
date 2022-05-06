package apr.aprlab.repair.adapt.ged.util;

import apr.aprlab.repair.adapt.entities.MyUnit;
import apr.aprlab.repair.adapt.ged.Constants;

public class GraphComponent {

    public enum ActionLabel {

        DEL, INS, MOV, UPD, NULL;

        public int length() {
            if (this == NULL) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    protected ActionLabel plotLabel = ActionLabel.NULL;

    private boolean isNode;

    protected MyUnit attribute = null;

    private String componentId;

    public int id;

    public boolean belongtosourcegraph;

    public GraphComponent() {
    }

    public GraphComponent(String id) {
        this.componentId = id;
    }

    public void setAttribute(MyUnit myUnit) {
        this.attribute = myUnit;
    }

    public MyUnit getAttribute() {
        return attribute;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public boolean isNode() {
        return isNode;
    }

    public void setIsNode(boolean isNode) {
        this.isNode = isNode;
    }

    public boolean isNullEps() {
        boolean is = getComponentId().equals(Constants.EPS_ID);
        return is;
    }

    public ActionLabel getPlotLabel() {
        return plotLabel;
    }

    public void setPlotLabel(ActionLabel plotLabel) {
        this.plotLabel = plotLabel;
    }

    public String getPlotLabelString() {
        if (plotLabel == ActionLabel.NULL) {
            return "";
        } else {
            return String.format("(%s)", plotLabel);
        }
    }

    public String getColorString() {
        String insColor = "deeppink";
        String updColor = "chocolate4";
        String movColor = "chartreuse2";
        String delColor = "gray";
        String formatString = " color=\"%s\"  fontcolor=\"%s\"";
        String colorString = "";
        if (plotLabel.length() > 0) {
            if (plotLabel == ActionLabel.INS) {
                colorString = String.format(formatString, insColor, insColor);
            } else if (plotLabel == ActionLabel.UPD) {
                colorString = String.format(formatString, updColor, updColor);
            } else if (plotLabel == ActionLabel.MOV) {
                colorString = String.format(formatString, movColor, movColor);
            } else if (plotLabel == ActionLabel.DEL) {
                colorString = String.format(formatString, delColor, delColor);
            }
        }
        return colorString;
    }
}
