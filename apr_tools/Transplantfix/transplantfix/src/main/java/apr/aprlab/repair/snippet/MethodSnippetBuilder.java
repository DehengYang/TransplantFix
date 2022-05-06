package apr.aprlab.repair.snippet;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import apr.aprlab.repair.fl.SuspiciousMethod;
import apr.aprlab.repair.search.CuInfo;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.utils.ast.ASTUtil;

public class MethodSnippetBuilder {

    private SuspiciousMethod sm;

    public MethodSnippetBuilder(SuspiciousMethod sm) {
        this.sm = sm;
    }

    public MethodSnippet create() {
        String className = sm.getClassName();
        TypeDeclaration td = ASTUtil.getParentTd(sm.getMethodDeclaration());
        CuInfo cuInfo = SearchTypeHierarchy.getCuInfoByTd(className, td);
        MethodDeclaration md = sm.getMethodDeclaration();
        MethodSnippet ms = cuInfo.getMethodSnippetByMd(md);
        ms.setSuspiciousMethod(sm);
        return ms;
    }
}
