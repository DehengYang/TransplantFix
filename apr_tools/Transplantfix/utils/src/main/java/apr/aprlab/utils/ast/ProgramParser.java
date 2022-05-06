package apr.aprlab.utils.ast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.Pair;
import apr.aprlab.utils.general.RegexUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProgramParser {

    public static final Logger logger = LogManager.getLogger(ProgramParser.class);

    public static Map<String, CompilationUnit> getCompilationUnitsFromProgram(String srcJavaDir, List<String> depList) {
        Map<String, CompilationUnit> cuMap = new HashMap<>();
        Collection<File> files = FileUtils.listFiles(new File(srcJavaDir), new String[] { "java" }, true);
        logger.debug("number of files in {}: {}", srcJavaDir, files.size());
        for (File file : files) {
            CompilationUnit cu = MyASTParser.parseFileToAST(file.getAbsolutePath(), depList, srcJavaDir);
            List<TypeDeclaration> tdList = new ArrayList<TypeDeclaration>();
            ASTWalk.findall(cu, TypeDeclaration.class, tdList::add);
            for (TypeDeclaration td : tdList) {
                ITypeBinding binding = td.resolveBinding();
                String className = binding.getQualifiedName();
                cuMap.putIfAbsent(className, cu);
            }
        }
        return cuMap;
    }

    public static Map<String, CompilationUnit> obfuscate(Map<String, CompilationUnit> cuMap) {
        Map<String, CompilationUnit> cuObfuscateMap = new HashMap<>();
        for (String className : cuMap.keySet()) {
            CompilationUnit cu = cuMap.get(className);
            Iterable<ASTNode> cuIterable = ASTWalk.getNodeIterable(cu);
            for (ASTNode ast : cuIterable) {
                if (ast instanceof SimpleName) {
                    ((SimpleName) ast).setIdentifier("ID");
                }
            }
            cuObfuscateMap.putIfAbsent(className, cu);
        }
        return cuObfuscateMap;
    }

    public static Map<String, List<Pair<String, String>>> obfuscate(String srcJavaDir) {
        Map<String, List<Pair<String, String>>> fileLinesMap = new HashMap<String, List<Pair<String, String>>>();
        Collection<File> files = FileUtils.listFiles(new File(srcJavaDir), new String[] { "java" }, true);
        logger.debug("number of files in {}: {}", srcJavaDir, files.size());
        String srcJavaDirPath = FileUtil.getCanonicalPath(srcJavaDir);
        for (File file : files) {
            String filePath = FileUtil.getCanonicalPath(file.toPath());
            String className = FileUtil.getClassNameFromPath(srcJavaDirPath, filePath);
            fileLinesMap.putIfAbsent(className, new ArrayList<Pair<String, String>>());
            String fileContent = FileUtil.readFileToStr(file);
            String formattedfileContent = RegexUtil.getFormattedString(fileContent);
            List<String> lineList = Arrays.asList(fileContent.split("\n"));
            List<String> formattedLineList = Arrays.asList(formattedfileContent.split("\n"));
            if (lineList.size() != formattedLineList.size()) {
                ExceptionUtil.raise("inequal sizes.");
            }
            for (int i = 0; i < lineList.size(); i++) {
                fileLinesMap.get(className).add(new Pair<>(lineList.get(i), formattedLineList.get(i)));
            }
        }
        return fileLinesMap;
    }
}
