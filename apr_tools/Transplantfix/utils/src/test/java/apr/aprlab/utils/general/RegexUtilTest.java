package apr.aprlab.utils.general;

import static org.junit.Assert.assertFalse;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegexUtilTest {

    public static final Logger logger = LogManager.getLogger(RegexUtil.class);

    @Test
    public void testRegex() {
        String string = "this.<org.jfree.chart.renderer.category.AbstractCategoryItemRenderer: org.jfree.chart.plot.CategoryPlot plot>";
        List<String> matches = RegexUtil.findAll(string, Pattern.compile("(.*?)\\.<.* (.*?)>"));
        assertFalse(matches.isEmpty());
        logger.debug("matches: {}", matches);
    }
}
