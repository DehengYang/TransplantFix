package apr.aprlab.repair.search;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.ast.ASTWalk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CuInfo {

    public static final Logger logger = LogManager.getLogger(CuInfo.class);

    private CompilationUnit compilationUnit;

    private TypeDeclaration typeDeclaration;

    private String filePath;

    private List<MethodDeclaration> methodDeclarations = new ArrayList<>();

    private List<MethodSnippet> methodSnippets = new ArrayList<>();

    public CuInfo(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, String filePath, String srcJavaDir, String className, String fileClassName, String fileString) {
        this.compilationUnit = compilationUnit;
        this.typeDeclaration = typeDeclaration;
        this.filePath = filePath;
        collectMethodDeclarations();
        collectMethodSnippets(srcJavaDir, className, fileClassName, fileString);
    }

    private void collectMethodSnippets(String srcJavaDir, String className, String fileClassName, String fileString) {
        for (MethodDeclaration md : methodDeclarations) {
            MethodSnippet ms = new MethodSnippet(Globals.msCnt++, srcJavaDir, className, fileClassName, compilationUnit, md, filePath, fileString);
            methodSnippets.add(ms);
        }
    }

    private void collectMethodDeclarations() {
        if (typeDeclaration != null) {
            List<MethodDeclaration> mds = new ArrayList<MethodDeclaration>();
            ASTWalk.findall(typeDeclaration, MethodDeclaration.class, mds::add);
            for (MethodDeclaration md : mds) {
                if (ASTUtil.getParentTd(md) == typeDeclaration) {
                    methodDeclarations.add(md);
                } else {
                }
            }
        } else {
            List<MethodDeclaration> mds = new ArrayList<MethodDeclaration>();
            ASTWalk.findall(compilationUnit, MethodDeclaration.class, mds::add);
            for (MethodDeclaration md : mds) {
                methodDeclarations.add(md);
            }
        }
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public String getFilePath() {
        return filePath;
    }

    public TypeDeclaration getTypeDeclaration() {
        return typeDeclaration;
    }

    public List<MethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public List<MethodSnippet> getMethodSnippets() {
        return methodSnippets;
    }

    public MethodSnippet getMethodSnippetByMd(MethodDeclaration md) {
        int index = methodDeclarations.indexOf(md);
        MethodSnippet ms = methodSnippets.get(index);
        return ms;
    }
}
