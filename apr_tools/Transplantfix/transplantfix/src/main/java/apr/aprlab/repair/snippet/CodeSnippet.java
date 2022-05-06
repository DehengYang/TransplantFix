package apr.aprlab.repair.snippet;

import org.eclipse.jdt.core.dom.CompilationUnit;
import apr.aprlab.utils.ast.Range;
import apr.aprlab.utils.general.StringUtil;

public abstract class CodeSnippet {

    protected String srcJavaDir;

    protected String className;

    protected String simpleClassName;

    protected CompilationUnit compilationUnit;

    protected Range range;

    protected float similarity;

    protected String filePath;

    public CodeSnippet(String srcJavaDir, String className, CompilationUnit cu, String filePath) {
        this.srcJavaDir = srcJavaDir;
        this.className = className;
        this.compilationUnit = cu;
        this.simpleClassName = StringUtil.getShortName(className);
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return String.format("srcJavaDir: %s, className: %s\n", srcJavaDir, className);
    }

    public String getSrcJavaDir() {
        return srcJavaDir;
    }

    public void setSrcJavaDir(String srcJavaDir) {
        this.srcJavaDir = srcJavaDir;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Range getRange() {
        return range;
    }

    public abstract float compareSimilarity(CodeSnippet cs);

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public String getSimpleClassName() {
        return simpleClassName;
    }
}
