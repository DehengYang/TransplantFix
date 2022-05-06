package apr.aprlab.repair.snippet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import apr.aprlab.repair.adapt.SootOriUtil;
import apr.aprlab.repair.adapt.entities.EntityInMethod;
import apr.aprlab.repair.adapt.entities.MethodName;
import apr.aprlab.repair.adapt.entities.MyUnit;
import apr.aprlab.repair.adapt.entities.MyUnitUtil;
import apr.aprlab.repair.adapt.entities.TypeName;
import apr.aprlab.repair.adapt.entities.VariableDef;
import apr.aprlab.repair.adapt.entities.VariableName;
import apr.aprlab.repair.adapt.ged.action.extract.GraphUtil;
import apr.aprlab.repair.adapt.ged.util.Edge;
import apr.aprlab.repair.adapt.ged.util.Graph;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.fl.SuspiciousMethod;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.repair.snippet.signature.MethodSignature;
import apr.aprlab.utils.ast.ASTWalk;
import apr.aprlab.utils.ast.Range;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.graph.HyperGraph;
import apr.aprlab.utils.graph.ProgramEdge;
import apr.aprlab.utils.graph.ProgramNode;
import apr.aprlab.utils.graph.cfg.CFG;
import apr.aprlab.utils.graph.ddg.MethodCall;
import apr.aprlab.utils.graph.printer.GraphPrinter;
import soot.SootMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodSnippet extends CodeSnippet {

    public static final Logger logger = LogManager.getLogger(CodeSnippet.class);

    private MethodDeclaration methodDeclaration;

    private HyperGraph hyperGraph;

    private Graph gedGraph;

    private List<SimpleName> simpleNames = new ArrayList<>();

    private Map<String, List<MethodName>> methodNameMap = new HashMap<>();

    private Map<String, List<TypeName>> typeNameMap = new HashMap<>();

    private Map<String, List<VariableDef>> varDefMap = new HashMap<>();

    private EntityInMethod entityInMethod;

    private Map<String, List<VariableName>> varNameMap = new HashMap<>();

    private List<ASTNode> methodSimpleNameNodes = new ArrayList<ASTNode>();

    private List<ASTNode> typeSimpleNameNodes = new ArrayList<ASTNode>();

    private String fileString;

    private List<ASTNode> astNodes = new ArrayList<ASTNode>();

    private MethodSignature methodSignature;

    private SuspiciousMethod suspiciousMethod;

    private int id;

    public MethodSnippet(int id, String srcJavaDir, String className, String fileClassName, CompilationUnit cu, MethodDeclaration md, String filePath, String fileString) {
        super(srcJavaDir, className, cu, filePath);
        this.id = id;
        for (ASTNode node : ASTWalk.getNodeIterable(md)) {
            astNodes.add(node);
        }
        this.range = RangeUtil.getRange(this.compilationUnit, md);
        this.methodDeclaration = md;
        this.methodSignature = new MethodSignature(md, className);
        this.entityInMethod = new EntityInMethod(getSimpleClassName());
        this.fileString = fileString;
    }

    public boolean setGraphForMethodSnippet(boolean saveAndPlot, boolean isSuspMethodSnippet) {
        if (getHyperGraph() != null) {
            logger.info("setGraphForMethodSnippet of this method snippet already done.");
            if (saveAndPlot) {
                saveAndPlot(className, hyperGraph, isSuspMethodSnippet);
            }
            return true;
        }
        SootMethod tgtSootMethod = SootOriUtil.getTargetMethod(className, this);
        if (tgtSootMethod == null) {
            logger.warn("tgtSootMethod is null: {}", this);
        } else {
            hyperGraph = new HyperGraph(tgtSootMethod, this.getMethodDeclaration());
            if (saveAndPlot) {
                saveAndPlot(className, hyperGraph, isSuspMethodSnippet);
            }
        }
        return false;
    }

    @SuppressWarnings({ "rawtypes" })
    private Graph transfer(CFG cfg, int gedGraphStartIndex, boolean isSuspMethodSnippet) {
        Graph graph = new Graph();
        String edgemode = "directed";
        graph.setEdgeMode(edgemode);
        Map<String, MyUnit> unitMap = new HashMap<>();
        int i = gedGraphStartIndex;
        for (ProgramNode tmpNode : cfg.getNodes()) {
            tmpNode.setId(String.valueOf(i));
            MyUnit myUnit = new MyUnit(tmpNode, this);
            unitMap.put(String.valueOf(i), myUnit);
            Node gedNode = new Node(true, String.valueOf(i), myUnit);
            graph.add(gedNode);
            i++;
        }
        for (ProgramEdge tmpEdge : cfg.getEdges()) {
            Edge gedEdge = new Edge(true, tmpEdge.getLabel());
            String fromId = tmpEdge.getSrc().getId();
            String toId = tmpEdge.getTgt().getId();
            gedEdge.setComponentId(fromId + "==" + toId);
            Iterator nodeIterator = graph.iterator();
            while (nodeIterator.hasNext()) {
                Node node = (Node) nodeIterator.next();
                if (node.getComponentId().equals(fromId)) {
                    gedEdge.setStartNode(node);
                    node.getEdges().add(gedEdge);
                }
                if (node.getComponentId().equals(toId)) {
                    gedEdge.setEndNode(node);
                    node.getEdges().add(gedEdge);
                }
            }
            graph.getEdges().add(gedEdge);
        }
        Globals.gedGraphStartIndex += graph.getNodes().size();
        graph.initFromToEdges();
        MyUnitUtil.deleteExtraGraphs(graph);
        MyUnitUtil.removeCaughtExceptionNodes(graph);
        graph.initFromToEdges();
        MyUnitUtil.addThisStartNode(graph);
        MyUnitUtil.removeBadLineNoNodes(graph);
        MyUnitUtil.removeOutOfMethodRangeNodes(graph, range);
        MyUnitUtil.updateLineNo(graph);
        MyUnitUtil.labelOneChildIf(graph);
        MyUnitUtil.labelWhileNodes(graph);
        MyUnitUtil.updateLineNoForElseIf(graph);
        if (Globals.debugMode) {
            saveAndPlot(className, graph, 1, isSuspMethodSnippet);
        }
        MyUnitUtil.identifyFakeOneLineIfNodes(graph);
        if (Globals.debugMode) {
            saveAndPlot(className, graph, 2, isSuspMethodSnippet);
        }
        MyUnitUtil.mergeMultiIfExprNodes(graph);
        if (Globals.debugMode) {
            saveAndPlot(className, graph, 3, isSuspMethodSnippet);
        }
        MyUnitUtil.identifyFakeIfNodes(graph);
        if (Globals.debugMode) {
            saveAndPlot(className, graph, 4, isSuspMethodSnippet);
        }
        MyUnitUtil.alignAstNodes(graph);
        if (Globals.debugMode) {
            saveAndPlot(className, graph, 5, isSuspMethodSnippet);
        }
        MyUnitUtil.removeNullAstNodes(graph);
        MyUnitUtil.addOmittedAstNodes(graph);
        saveAndPlot(className, graph, 6, isSuspMethodSnippet);
        return graph;
    }

    private void saveAndPlot(String className, Graph gedGraph, int index, boolean isSuspMethodSnippet) {
        String dotFilePrefix = getDotFilePrefix();
        if (isSuspMethodSnippet) {
            GraphUtil.plot(gedGraph, Globals.suspGraphDir, dotFilePrefix + "gedGraph__" + index + ".dot");
        } else {
            GraphUtil.plot(gedGraph, Globals.donorGraphDir, dotFilePrefix + "gedGraph__" + index + ".dot");
        }
    }

    private void saveAndPlot(String className, HyperGraph hg, boolean isSuspMethodSnippet) {
        String dotFilePrefix = getDotFilePrefix();
        if (isSuspMethodSnippet) {
            GraphPrinter.plot(hg.getCfg(), Globals.suspGraphDir, dotFilePrefix + "cfg.dot");
        } else {
            GraphPrinter.plot(hg.getCfg(), Globals.donorGraphDir, dotFilePrefix + "cfg.dot");
        }
    }

    public String getDotFilePrefix() {
        String dotFilePrefix = String.format("%s_%s_[%s_%s]_", StringUtil.getShortName(className), this.getMethodName(), range.getStartLineNo(), range.getEndLineNo());
        return dotFilePrefix;
    }

    @Override
    public float compareSimilarity(CodeSnippet cs) {
        if (cs instanceof MethodSnippet) {
            MethodSnippet ms = (MethodSnippet) cs;
            float sim = this.methodSignature.compareSimilarity(ms.getMethodSignature());
            return sim;
        } else {
            return 0;
        }
    }

    public String getMethodName() {
        if (methodSignature != null) {
            String methodName = methodSignature.getMethodName().toString();
            return methodName;
        } else {
            ExceptionUtil.raise("methodSignature is null.");
        }
        return null;
    }

    public void collectEntitiesInMethod() {
        List<ASTNode> traversedNodes = ASTWalk.getNodeIterableExcludeJavadoc(methodDeclaration);
        List<MethodCall> methodCalls = hyperGraph.getCfg().getMethodCalls();
        List<MethodCall> visitedMcs = new ArrayList<MethodCall>();
        for (ASTNode node : traversedNodes) {
            if (methodDeclaration.getName() == node) {
                continue;
            }
            collectAPIsInMethod(node, methodNameMap, methodSimpleNameNodes, methodCalls, visitedMcs);
            collectTypesInMethod(node, typeNameMap, typeSimpleNameNodes);
            collectSimpleNameInMethod(node, simpleNames);
        }
        Iterable<ASTNode> bodyIter = ASTWalk.getNodeIterable(methodDeclaration.getBody());
        for (ASTNode node : bodyIter) {
            collectVarDefsInMethodBody(node, varDefMap);
        }
        collectVarDefsInMethodParameters(varDefMap);
        collectFieldVarDefsInMethod(varDefMap);
        collectVarsInMethod(simpleNames, varNameMap, methodSimpleNameNodes, typeSimpleNameNodes, varDefMap);
    }

    private void collectTypesInMethod(ASTNode node, Map<String, List<TypeName>> typeNameMap2, List<ASTNode> typeSimpleNameNodes2) {
        if (node instanceof SimpleType) {
            String typeName = ((SimpleType) node).getName().toString();
            CollectionUtil.addToMap(typeNameMap2, typeName, new TypeName(typeName, node));
            typeSimpleNameNodes2.add(((SimpleType) node).getName());
            entityInMethod.addType(typeName, node);
        } else if (node instanceof MethodInvocation) {
            Expression expr = ((MethodInvocation) node).getExpression();
            if (expr instanceof SimpleName) {
                String typeName = ((SimpleName) expr).getIdentifier();
                if (Character.isUpperCase(typeName.charAt(0))) {
                    CollectionUtil.addToMap(typeNameMap2, typeName, new TypeName(typeName, (SimpleName) expr));
                    typeSimpleNameNodes2.add(expr);
                    entityInMethod.addType(typeName, expr);
                }
            }
        }
    }

    private void collectAPIsInMethod(ASTNode node, Map<String, List<MethodName>> methodNameMap2, List<ASTNode> methodSimpleNameNodes2, List<MethodCall> methodCalls, List<MethodCall> visitedMcs) {
        Range miRange = null;
        String apiName = null;
        ASTNode methodSimpleName = null;
        ASTNode wholeMethodNode = null;
        if (node instanceof MethodInvocation) {
            MethodInvocation mi = (MethodInvocation) node;
            wholeMethodNode = mi;
            methodSimpleName = mi.getName();
            apiName = mi.getName().getIdentifier();
            miRange = RangeUtil.getRange(compilationUnit, mi);
        } else if (node instanceof SuperMethodInvocation) {
            SuperMethodInvocation mi = (SuperMethodInvocation) node;
            wholeMethodNode = mi;
            apiName = mi.getName().getIdentifier();
            miRange = RangeUtil.getRange(compilationUnit, mi);
            methodSimpleName = mi.getName();
        }
        if (miRange == null || apiName == null) {
            return;
        }
        for (MethodCall mc : methodCalls) {
            if (!visitedMcs.contains(mc)) {
                if (methodSimpleName instanceof Type) {
                    if (RangeUtil.lineInRange(miRange, mc.getLineNo()) && "<init>".equals(mc.getMethodName())) {
                        CollectionUtil.addToMap(methodNameMap2, mc.getMethodCallSignature(), new MethodName(apiName, wholeMethodNode, mc));
                        visitedMcs.add(mc);
                        entityInMethod.addAPI(methodSimpleName, wholeMethodNode, mc);
                        break;
                    }
                } else {
                    if (RangeUtil.lineInRange(miRange, mc.getLineNo()) && apiName.equals(mc.getMethodName())) {
                        CollectionUtil.addToMap(methodNameMap2, mc.getMethodCallSignature(), new MethodName(methodSimpleName, wholeMethodNode, mc));
                        methodSimpleNameNodes2.add(methodSimpleName);
                        visitedMcs.add(mc);
                        entityInMethod.addAPI(methodSimpleName, wholeMethodNode, mc);
                        break;
                    }
                }
            }
        }
    }

    private void collectSimpleNameInMethod(ASTNode node, List<SimpleName> simpleNames) {
        if (node instanceof SimpleName) {
            simpleNames.add((SimpleName) node);
        }
    }

    private void collectVarDefsInMethodParameters(Map<String, List<VariableDef>> varDefMap2) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> svds = methodDeclaration.parameters();
        for (SingleVariableDeclaration svd : svds) {
            String varName = ((SingleVariableDeclaration) svd).getName().getIdentifier();
            String varType = svd.getType().toString();
            CollectionUtil.addToMap(varDefMap2, varName, new VariableDef(svd, varName, varType));
        }
    }

    private void collectVarDefsInMethodBody(ASTNode node, Map<String, List<VariableDef>> varDefMap2) {
        if (node instanceof VariableDeclarationExpression) {
            VariableDeclarationExpression vde = (VariableDeclarationExpression) node;
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fragments = vde.fragments();
            Type type = vde.getType();
            for (VariableDeclarationFragment vdf : fragments) {
                String varName = vdf.getName().toString();
                CollectionUtil.addToMap(varDefMap2, varName, new VariableDef(vdf, varName, type.toString()));
            }
        } else if (node instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement vds = (VariableDeclarationStatement) node;
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fragments = vds.fragments();
            Type type = vds.getType();
            for (VariableDeclarationFragment vdf : fragments) {
                String varName = vdf.getName().toString();
                CollectionUtil.addToMap(varDefMap2, varName, new VariableDef(vdf, varName, type.toString()));
            }
        } else if (node instanceof SingleVariableDeclaration) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) node;
            String varName = svd.getName().toString();
            String typeName = svd.getType().toString();
            CollectionUtil.addToMap(varDefMap2, varName, new VariableDef(svd, varName, typeName));
        }
    }

    private void collectFieldVarDefsInMethod(Map<String, List<VariableDef>> varDefMap2) {
        for (SimpleName simpleNameNode : simpleNames) {
            String simpleName = simpleNameNode.getIdentifier();
            IBinding binding = simpleNameNode.resolveBinding();
            if (SearchTypeHierarchy.classnameToFieldMap.get(className).containsKey(simpleName)) {
                if (binding == null) {
                    logger.error("todo (may not be a field): binding is null for node: {} & its parent: {}", simpleNameNode, simpleNameNode.getParent());
                    continue;
                }
                if (binding.getKind() == IBinding.VARIABLE) {
                    IVariableBinding bind = (IVariableBinding) binding;
                    if (!bind.isField()) {
                        logger.error("binding is not field binding: {}", binding);
                        continue;
                    }
                }
                VariableDeclarationFragment node = (VariableDeclarationFragment) SearchTypeHierarchy.classnameToFieldMap.get(className).get(simpleName);
                FieldDeclaration fd = (FieldDeclaration) node.getParent();
                String varType = fd.getType().toString();
                VariableDef varDef = new VariableDef(fd, simpleName, varType);
                CollectionUtil.addToMap(varDefMap2, simpleName, varDef);
            }
        }
    }

    private void collectVarsInMethod(List<SimpleName> simpleNames2, Map<String, List<VariableName>> varNameMap2, List<ASTNode> methodSimpleNameNodes2, List<ASTNode> typeSimpleNameNodes2, Map<String, List<VariableDef>> varDefMap) {
        for (SimpleName node : simpleNames2) {
            if (!methodSimpleNameNodes2.contains(node) && !typeSimpleNameNodes2.contains(node)) {
                String varName = node.getIdentifier();
                if (varDefMap.get(varName) == null) {
                    continue;
                }
                if (varDefMap.get(varName).size() != 1) {
                    SnippetUtil.checkVarDefConsistency(varDefMap.get(varName));
                }
                VariableDef varDef = varDefMap.get(varName).get(0);
                CollectionUtil.addToMap(varNameMap2, varName, new VariableName(node, varDef));
                entityInMethod.addVar(varName, node, varDef);
            }
        }
    }

    public HyperGraph getHyperGraph() {
        return hyperGraph;
    }

    @Override
    public String toString() {
        return String.format("====================\n" + "similarity: %s, methodSignature: %s, \n\nmd <%s>:\n %s" + "====================\n\n", this.getSimilarity(), methodSignature.toString(), range.toString(), methodDeclaration.toString());
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public MethodSignature getMethodSignature() {
        return methodSignature;
    }

    public Graph getGedGraph() {
        return gedGraph;
    }

    public List<ASTNode> getAstNodes() {
        return astNodes;
    }

    public Map<String, List<MethodName>> getMethodNameMap() {
        return methodNameMap;
    }

    public Map<String, List<TypeName>> getTypeNameMap() {
        return typeNameMap;
    }

    public Map<String, List<VariableDef>> getVarDefMap() {
        return varDefMap;
    }

    public Map<String, List<VariableName>> getVarNameMap() {
        return varNameMap;
    }

    public List<ASTNode> getMethodSimpleNameNodes() {
        return methodSimpleNameNodes;
    }

    public List<ASTNode> getTypeSimpleNameNodes() {
        return typeSimpleNameNodes;
    }

    public List<SimpleName> getSimpleNames() {
        return simpleNames;
    }

    public String getFileString() {
        return fileString;
    }

    public void constructGedGraph(boolean isSuspMethodSnippet, boolean saveAndPlot, int gedGraphStartIndex) {
        gedGraph = transfer(hyperGraph.getCfg(), gedGraphStartIndex, isSuspMethodSnippet);
    }

    public void contructAndCollect(boolean isSuspMethodSnippet) {
        boolean alreadyDone = setGraphForMethodSnippet(true, isSuspMethodSnippet);
        if (alreadyDone) {
            return;
        } else {
            if (hyperGraph == null) {
                return;
            }
            collectEntitiesInMethod();
            constructGedGraph(isSuspMethodSnippet, true, Globals.gedGraphStartIndex);
        }
    }

    public EntityInMethod getEntityInMethod() {
        return entityInMethod;
    }

    @Override
    public int hashCode() {
        return className.hashCode() + range.getStartLineNo() + range.getEndLineNo();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        } else if (getClass() != obj.getClass()) {
            return false;
        } else {
            MethodSnippet fsOther = (MethodSnippet) obj;
            return range.toString().equals(fsOther.getRange().toString()) && methodDeclaration.toString().equals(fsOther.getMethodDeclaration().toString()) && filePath.equals(fsOther.getFilePath());
        }
    }

    public void setSuspiciousMethod(SuspiciousMethod sm) {
        this.suspiciousMethod = sm;
    }

    public SuspiciousMethod getSuspiciousMethod() {
        return suspiciousMethod;
    }

    public void clearMyUnitMappings() {
        List<Node> nodes = gedGraph.getNodes();
        for (Node node : nodes) {
            MyUnit myUnit = node.getOldAttribute();
            myUnit.clearMappings();
        }
    }

    public int getId() {
        return id;
    }
}
