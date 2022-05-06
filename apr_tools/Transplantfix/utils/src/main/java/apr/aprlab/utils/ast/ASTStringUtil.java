package apr.aprlab.utils.ast;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTStringUtil {

    public static List<String> getFormattedLines(CompilationUnit cu) {
        List<String> lineList = new ArrayList<String>();
        String cuStr = cu.toString();
        for (String line : cuStr.split("\n")) {
            String formattedLine = line.trim().replaceAll(" +", " ");
            lineList.add(formattedLine);
        }
        return lineList;
    }
}
