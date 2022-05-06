package apr.aprlab.utils.ast;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import apr.aprlab.utils.general.FileUtil;

public class MyASTParser {

    public static final Logger logger = LogManager.getLogger(MyASTParser.class);

    @SuppressWarnings({ "unchecked", "deprecation" })
    public static CompilationUnit fastParseFileToAST(String filePath) {
        ASTParser parser = ASTParser.newParser(AST.JLS14);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        @SuppressWarnings("rawtypes")
        Map options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        parser.setCompilerOptions(options);
        char[] sourceCode = FileUtil.readFileToCharArray(filePath);
        parser.setSource(sourceCode);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        return cu;
    }

    public static CompilationUnit parseFileToAST(char[] sourceCode, String filePath, List<String> dependencyPaths, String srcJavaDir) {
        return parseFileToAST(sourceCode, filePath, dependencyPaths.toArray(String[]::new), srcJavaDir);
    }

    public static CompilationUnit parseFileToAST(String filePath, List<String> dependencyPaths, String srcJavaDir) {
        return parseFileToAST(filePath, dependencyPaths.toArray(String[]::new), srcJavaDir);
    }

    public static CompilationUnit parseFileToAST(File file, String[] dependencyPaths, String srcJavaDir) {
        return parseFileToAST(file.getAbsolutePath(), dependencyPaths, srcJavaDir);
    }

    public static CompilationUnit parseFileToAST(String filePath, String[] dependencyPaths, String srcJavaDir) {
        char[] sourceCode = FileUtil.readFileToCharArray(filePath);
        return parseFileToAST(sourceCode, filePath, dependencyPaths, srcJavaDir);
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public static CompilationUnit parseFileToAST(char[] sourceCode, String filePath, String[] dependencyPaths, String srcJavaDir) {
        ASTParser parser = ASTParser.newParser(AST.JLS14);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        Map options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_14);
        options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
        parser.setCompilerOptions(options);
        if (srcJavaDir == null) {
            parser.setEnvironment(dependencyPaths, null, new String[] { "UTF-8" }, true);
        } else {
            parser.setEnvironment(dependencyPaths, new String[] { srcJavaDir }, new String[] { "UTF-8" }, true);
        }
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(false);
        parser.setSource(sourceCode);
        String name = Paths.get(filePath).toFile().getName();
        parser.setUnitName(name);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        return cu;
    }
}
