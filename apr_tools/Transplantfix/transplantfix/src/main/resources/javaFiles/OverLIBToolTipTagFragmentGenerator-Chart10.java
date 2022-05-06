package org.jfree.chart.imagemap;

public class OverLIBToolTipTagFragmentGenerator implements ToolTipTagFragmentGenerator {

    public OverLIBToolTipTagFragmentGenerator() {
        super();
    }

    public String generateToolTipFragment(String toolTipText) {
        return " onMouseOver=\"return overlib('" + ImageMapUtilities.htmlEscape(toolTipText) + "');\" onMouseOut=\"return nd();\"";
    }
}
