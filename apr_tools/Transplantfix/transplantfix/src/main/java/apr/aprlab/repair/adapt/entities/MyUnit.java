package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.Pair;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.graph.ProgramNode;
import soot.Unit;
import soot.jimple.internal.AbstractDefinitionStmt;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JTableSwitchStmt;
import soot.jimple.internal.JThrowStmt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class MyUnit {

    public static final Logger logger = LogManager.getLogger(MyUnit.class);

    private ASTNode astNode;

    private Unit unit;

    private int lineNo;

    private int unitId;

    private MethodSnippet methodSnippet;

    private boolean isStack = false;

    private boolean isNumberSign = false;

    private boolean isIf = false;

    private boolean isSpecialInvokeMethod = false;

    private Map<String, String> oriToShortTypeMap = new HashMap<>();

    List<VariableName> vars = new ArrayList<VariableName>();

    List<TypeName> types = new ArrayList<TypeName>();

    List<MethodName> methods = new ArrayList<MethodName>();

    private Pair<Integer, Integer> posRange;

    private MappedStringClass mappedStringClass;

    private boolean isSwitch = false;

    private boolean isWhile = false;

    private boolean isField = false;

    private boolean isEnhancedFor = false;

    private boolean isStartThis = false;

    private boolean isStartPara = false;

    private boolean isLastReturn = false;

    private List<MappedStringClass> mappedStringClasses = new ArrayList<>();

    private List<MappedStringClass> extraMappedStringClasses = new ArrayList<>();

    public MyUnit(ProgramNode node, MethodSnippet methodSnippet) {
        this.unit = node.getUnit();
        this.unitId = Integer.parseInt(node.getId());
        this.methodSnippet = methodSnippet;
        this.lineNo = unit.getJavaSourceStartLineNumber();
        checkThisParaAndField(lineNo);
        setFieldsFromUnit();
    }

    public MyUnit(ASTNode astNode, MethodSnippet methodSnippet) {
        this.methodSnippet = methodSnippet;
        if (astNode != null) {
            this.lineNo = RangeUtil.getStartLineNo(methodSnippet.getCompilationUnit(), astNode);
            this.astNode = astNode;
        }
    }

    public MyUnit(ASTNode astNode, MethodSnippet methodSnippet, boolean isStartThis) {
        this(astNode, methodSnippet);
        ExceptionUtil.myAssert(isStartThis);
        this.isStartThis = isStartThis;
        this.lineNo = -1;
    }

    private void checkThisParaAndField(int lineNo) {
        if (lineNo == -1) {
            if (unit.toString().contains(" := @this:")) {
                isStartThis = true;
            } else if (unit.toString().contains(" := @parameter")) {
                isStartPara = true;
            } else {
                logger.warn("unknown unit with -1 lineno: {}", this.toString());
            }
        } else {
            if (!RangeUtil.lineInRange(methodSnippet.getMethodDeclaration(), lineNo)) {
                logger.warn("[isField] unit: {}, lineNo: {} is out of the method.", unit, lineNo);
                isField = true;
            }
        }
    }

    public void setFieldsFromUnit() {
        String string = unit.toString();
        if (unit instanceof AbstractDefinitionStmt) {
            if (string.contains("#")) {
                isNumberSign = true;
            }
            if (string.startsWith("$stack")) {
                isStack = true;
            }
        } else if (unit instanceof JInvokeStmt) {
            if (unit.toString().startsWith("specialinvoke ")) {
                isSpecialInvokeMethod = true;
            }
        } else if (unit instanceof JIfStmt) {
            isIf = true;
        } else if (unit instanceof JReturnStmt || unit instanceof JThrowStmt || unit instanceof JReturnVoidStmt || unit instanceof JEnterMonitorStmt || unit instanceof JExitMonitorStmt) {
        } else if (unit instanceof JLookupSwitchStmt || unit instanceof JTableSwitchStmt) {
            isSwitch = true;
        } else {
            logger.debug("unit class: {}, unit string: {}, lineNo: {}", unit.getClass(), unit.toString(), lineNo);
            ExceptionUtil.raise();
        }
    }

    public Unit getUnit() {
        return unit;
    }

    public int getLineNo() {
        return lineNo;
    }

    public int getUnitId() {
        return unitId;
    }

    @Override
    public String toString() {
        return String.format("[%s] unit: %s, astNode: %s", lineNo, unit, astNode);
    }

    public String toGraphString() {
        String astString = "";
        if (astNode != null) {
            String theAstString = astNode.toString();
            if (theAstString.length() > 100) {
                theAstString = theAstString.substring(0, 100);
            }
            astString = theAstString.trim().replace("\"", "");
            while (astString.endsWith("\\")) {
                astString = astString.substring(0, astString.length() - 1);
            }
        }
        return String.format("<%s> %s", lineNo, astString);
    }

    public ASTNode getAstNode() {
        return astNode;
    }

    private void setAllEntities(MethodSnippet methodSnippet) {
        List<Entity> entities = methodSnippet.getEntityInMethod().getEntitiesInASTNode(astNode);
        for (Entity entity : entities) {
            if (entity instanceof VariableName) {
                vars.add((VariableName) entity);
            } else if (entity instanceof TypeName) {
                types.add((TypeName) entity);
            } else if (entity instanceof MethodName) {
                methods.add((MethodName) entity);
            } else {
                ExceptionUtil.raise();
            }
        }
    }

    public MethodSnippet getMethodSnippet() {
        return methodSnippet;
    }

    public Map<String, String> getOriToShortTypeMap() {
        return oriToShortTypeMap;
    }

    public String getAstString() {
        if (astNode == null) {
            return Globals.isStartThisOrPara;
        } else {
            int startPos = astNode.getStartPosition();
            int endPos = astNode.getStartPosition() + astNode.getLength();
            String fileString = methodSnippet.getFileString();
            return fileString.substring(startPos, endPos);
        }
    }

    public boolean isStack() {
        return isStack;
    }

    public boolean isIf() {
        return isIf;
    }

    public boolean isStmt() {
        if (isLastReturn) {
            return true;
        }
        ExceptionUtil.assertNotNull(astNode);
        boolean is = astNode instanceof Statement;
        return is;
    }

    public Pair<Integer, Integer> getPosRange() {
        if (posRange != null) {
            return posRange;
        }
        if (isLastReturn) {
            Pair<Integer, Integer> mdRange = RangeUtil.getPosRange(methodSnippet.getCompilationUnit(), methodSnippet.getMethodDeclaration());
            posRange = new Pair<>(mdRange.getRight() - 1, mdRange.getRight() - 1);
            String string = methodSnippet.getFileString().substring(posRange.getLeft(), posRange.getRight());
            logger.debug("lastreutrn posRange: {}, string: {}", posRange, string);
        }
        posRange = RangeUtil.getPosRange(methodSnippet.getCompilationUnit(), astNode);
        return posRange;
    }

    public List<VariableName> getVars() {
        return vars;
    }

    public List<TypeName> getTypes() {
        return types;
    }

    public List<MethodName> getMethods() {
        return methods;
    }

    public ASTNode getIfCondition() {
        ExceptionUtil.assertTrue(isIf());
        ASTNode parent = astNode;
        while (!ASTUtil.isLoopOrIfAstNode(parent)) {
            parent = parent.getParent();
        }
        ExceptionUtil.assertNotNull(parent);
        return parent;
    }

    public void updateLineNo(int differentPrevLineNo) {
        lineNo = differentPrevLineNo;
    }

    public boolean isNumberSign() {
        return isNumberSign;
    }

    public boolean isSpecialInvokeMethod() {
        return isSpecialInvokeMethod;
    }

    public void setAstNodeAndCollectEntities(ASTNode astNode) {
        this.astNode = astNode;
        setAllEntities(methodSnippet);
    }

    public boolean isWhile() {
        return isWhile;
    }

    public void setIsWhile(boolean isWhile2) {
        this.isWhile = isWhile2;
    }

    public static Logger getLogger() {
        return logger;
    }

    public boolean isField() {
        return isField;
    }

    public boolean isStartThis() {
        return isStartThis;
    }

    public boolean isStartPara() {
        return isStartPara;
    }

    public boolean isStartThisOrPara() {
        return isStartThis || isStartPara;
    }

    public void setIsLastReturn(boolean isLastReturn) {
        this.isLastReturn = isLastReturn;
        Pair<Integer, Integer> range = RangeUtil.getPosRange(methodSnippet.getCompilationUnit(), methodSnippet.getMethodDeclaration());
        posRange = new Pair<>(range.getRight(), range.getRight());
    }

    public boolean isLastReturn() {
        return isLastReturn;
    }

    public Map<String, List<String>> getVarMapping() {
        return getMethodSnippet().getEntityInMethod().getVarMapping();
    }

    public void addMappingAndSetString(Map<String, String> newVarMapping) {
        List<Pair<ASTNode, String>> oldMappings = new ArrayList<>();
        Map<String, String> theNewVarMapping = new HashMap<>();
        if (mappedStringClass == null) {
            logger.warn("mappedStringClass is null. May be last return (with non-exist astnode).");
            return;
        }
        oldMappings.addAll(mappedStringClass.getMappings());
        theNewVarMapping = new HashMap<String, String>(mappedStringClass.getVarMapping());
        for (VariableName var : vars) {
            String varName = var.getVarName();
            if (newVarMapping.containsKey(varName)) {
                theNewVarMapping.put(varName, newVarMapping.get(varName));
                for (ASTNode node : var.getNodes()) {
                    oldMappings.add(new Pair<>(node, newVarMapping.get(varName)));
                }
            }
        }
        MappedStringClass mappedStringClass = new MappedStringClass(oldMappings, this, theNewVarMapping);
        if (!CollectionUtil.containsStringTrim(getMappedStrings(), mappedStringClass.getMappedString())) {
            mappedStringClasses.add(mappedStringClass);
        }
    }

    public ASTNode getIfStmt() {
        ExceptionUtil.myAssert(isIf && !isWhile);
        ASTNode parent = astNode.getParent();
        while (!(parent instanceof IfStatement)) {
            parent = parent.getParent();
        }
        return parent;
    }

    public boolean isSwitch() {
        return isSwitch;
    }

    public void setMappedStrings(MethodSnippet buggyMs) {
        String buggyReturnType = buggyMs.getMethodSignature().getReturnTypeString();
        if (astNode instanceof ReturnStatement) {
            String returnType = methodSnippet.getMethodSignature().getReturnTypeString();
            if (returnType.equals(buggyReturnType) || isMappedWithBuggyReturnType(returnType, buggyReturnType)) {
            } else {
                addReturnStmts(buggyMs);
                return;
            }
        }
        List<MappedStringClass> mappedStringClassesForVars = getMappedStringsForMatchedVars();
        ExceptionUtil.myAssert(mappedStringClassesForVars.size() >= 1);
        mappedStringClasses.addAll(mappedStringClassesForVars);
    }

    public void updateMappedStringClasses(MethodSnippet buggyMethodSnippet) {
        this.mappedStringClasses.addAll(extraMappedStringClasses);
        MappedStringClass extraMethodCallMappedStringClass = getExtraMethodCallMappingSrc();
        if (extraMethodCallMappedStringClass != null) {
            if (!getMappedStrings().contains(extraMethodCallMappedStringClass.getMappedString())) {
                mappedStringClasses.add(extraMethodCallMappedStringClass);
            }
        }
        if (astNode instanceof ThrowStatement) {
            mappedStringClasses.add(new MappedStringClass(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>(), astNode.toString(), this));
            addReturnStmts(buggyMethodSnippet);
        }
    }

    private void addReturnStmts(MethodSnippet buggyMs) {
        String buggyReturnType = buggyMs.getMethodSignature().getReturnTypeString();
        if (StringUtil.isPrimitiveType(buggyReturnType)) {
            String primitiveRetType = StringUtil.getPrimitiveString(buggyReturnType);
            mappedStringClasses.add(new MappedStringClass(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>(), String.format("return %s;", primitiveRetType), this));
            if (primitiveRetType.equals("false")) {
                mappedStringClasses.add(new MappedStringClass(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>(), "return true;", this));
            }
        } else {
            mappedStringClass = new MappedStringClass(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>(), "return null;", this);
            mappedStringClasses.add(mappedStringClass);
            String returnType = buggyMs.getMethodSignature().getReturnTypeString();
            for (SingleVariableDeclaration svd : buggyMs.getMethodSignature().getParameters()) {
                if (svd.getType().toString().equals(returnType)) {
                    String varName = svd.getName().toString();
                    mappedStringClass = new MappedStringClass(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>(), String.format("return %s;", varName), this);
                    mappedStringClasses.add(mappedStringClass);
                }
            }
        }
    }

    private boolean isMappedWithBuggyReturnType(String returnType, String buggyReturnType) {
        Map<String, String> typeMapping = methodSnippet.getEntityInMethod().getTypeMapping();
        if (typeMapping.containsKey(returnType)) {
            if (typeMapping.get(returnType).equals(buggyReturnType)) {
                return true;
            }
        }
        return false;
    }

    private MappedStringClass getExtraMethodCallMappingSrc() {
        List<Pair<ASTNode, String>> extraMcMappings = new ArrayList<>();
        Map<String, String> methodCallMapping = methodSnippet.getEntityInMethod().getMethodCallMapping();
        for (MethodName method : methods) {
            if (methodCallMapping.containsKey(method.getMethodName())) {
                for (ASTNode node : method.getWholeNodes()) {
                    extraMcMappings.add(new Pair<>(node, methodCallMapping.get(method.getMethodName())));
                }
            }
        }
        if (!extraMcMappings.isEmpty()) {
            extraMcMappings.sort(ComparatorUtil.astNodePairComparator);
            mergeMappings(extraMcMappings);
            MappedStringClass mappedStringClass = new MappedStringClass(extraMcMappings, this, null);
            return mappedStringClass;
        } else {
            return null;
        }
    }

    private void mergeMappings(List<Pair<ASTNode, String>> extraMcMappings) {
        List<Pair<ASTNode, String>> extraMcMappingsCpy = new ArrayList<Pair<ASTNode, String>>(extraMcMappings);
        for (Pair<ASTNode, String> pair : extraMcMappingsCpy) {
            for (Pair<ASTNode, String> curPair : extraMcMappingsCpy) {
                if (RangeUtil.fullyContainsDstNode(pair.getLeft(), curPair.getLeft())) {
                    logger.debug("remove child mapping: {}", curPair);
                    extraMcMappings.remove(curPair);
                }
            }
        }
    }

    public String getMappedString() {
        String finalString = getMappedStrings().get(0);
        if (finalString.length() == 0) {
            finalString = getAstString();
        } else {
            if (finalString.endsWith(";")) {
                finalString += "\n";
            }
        }
        return finalString;
    }

    private List<String> getMappedStrings() {
        List<String> mappedStrings = new ArrayList<String>();
        for (MappedStringClass mappedStringClass : mappedStringClasses) {
            mappedStrings.add(mappedStringClass.getMappedString());
        }
        return mappedStrings;
    }

    private List<String> getExtraMappedStrings() {
        List<String> mappedStrings = new ArrayList<String>();
        for (MappedStringClass mappedStringClass : extraMappedStringClasses) {
            mappedStrings.add(mappedStringClass.getMappedString());
        }
        return mappedStrings;
    }

    private List<MappedStringClass> getMappedStringsForMatchedVars() {
        Map<String, String> typeMapping = methodSnippet.getEntityInMethod().getTypeMapping();
        Map<String, String> methodMapping = methodSnippet.getEntityInMethod().getMethodMapping();
        Map<String, List<String>> varMapping = methodSnippet.getEntityInMethod().getVarMapping();
        if (astNode == null) {
            return getMappedStringsForNullAst();
        }
        List<MappedStringClass> mappedStringClasses = new ArrayList<MappedStringClass>();
        List<String> mappedStrings = new ArrayList<String>();
        int maxMappingCnt = MappingUtil.getMaxMappingCntForVar(vars, varMapping);
        for (int curCnt = 0; curCnt < maxMappingCnt; curCnt++) {
            MappedStringClass mappedStringClass = new MappedStringClass(typeMapping, methodMapping, varMapping, curCnt, this);
            mappedStringClasses.add(mappedStringClass);
            if (!CollectionUtil.containsStringTrim(mappedStrings, mappedStringClass.getMappedString())) {
                mappedStrings.add(mappedStringClass.getMappedString());
            }
        }
        String originalString = astNode.toString();
        if (!CollectionUtil.containsStringTrimAllWhiteSpaces(mappedStrings, originalString)) {
            MappedStringClass mappedStringClass = new MappedStringClass(new HashMap<String, String>(), new HashMap<String, String>(), new HashMap<String, String>(), originalString, this);
            mappedStrings.add(originalString);
            mappedStringClasses.add(mappedStringClass);
        }
        this.mappedStringClass = mappedStringClasses.get(0);
        return mappedStringClasses;
    }

    private List<MappedStringClass> getMappedStringsForNullAst() {
        if (isLastReturn) {
            return new ArrayList<>(Arrays.asList(new MappedStringClass("return;")));
        }
        if (mappedStringClass == null) {
            ExceptionUtil.myAssert(isStartThisOrPara());
            String mappedString = "isStartThisOrPara";
            return new ArrayList<>(Arrays.asList(new MappedStringClass(mappedString)));
        } else {
            ExceptionUtil.raise();
            return null;
        }
    }

    public MappedStringClass getMappedStringClass(int curPatternCnt, List<MappedStringClass> usedMappedStringClasses, MethodSnippet srcMethodSnippet) {
        MappedStringClass mappedStringClass = getMappedStringClass(curPatternCnt);
        if (MappingUtil.mappingConflictsOrVarNotExists(mappedStringClass, usedMappedStringClasses, srcMethodSnippet)) {
            MappedStringClass mappedStringClassNoConflict = MappingUtil.findNonConflictMappedStringClass(mappedStringClasses, usedMappedStringClasses, srcMethodSnippet);
            if (mappedStringClassNoConflict != null) {
                usedMappedStringClasses.add(mappedStringClassNoConflict);
                return mappedStringClassNoConflict;
            } else {
                return null;
            }
        } else {
            usedMappedStringClasses.add(mappedStringClass);
            return mappedStringClass;
        }
    }

    private MappedStringClass getMappedStringClass(int curPatternCnt) {
        if (curPatternCnt < mappedStringClasses.size()) {
            return mappedStringClasses.get(curPatternCnt);
        } else {
            return mappedStringClasses.get(0);
        }
    }

    public void setIsEnhancedFor(boolean isEnhancedFor) {
        this.isEnhancedFor = isEnhancedFor;
    }

    public boolean isEnhancedFor() {
        return isEnhancedFor;
    }

    public void addExtraNewString(Node startNode, String extraNewString) {
        MappedStringClass mappedStringClass = new MappedStringClass(extraNewString);
        mappedStringClass.setExtraMappedNode(startNode);
        if (!getMappedStrings().contains(extraNewString) && !getExtraMappedStrings().contains(extraNewString)) {
            extraMappedStringClasses.add(mappedStringClass);
        }
    }

    public MappedStringClass getMappedStringClass() {
        return mappedStringClass;
    }

    public List<MappedStringClass> getMappedStringClasses() {
        return mappedStringClasses;
    }

    public List<MappedStringClass> getExtraMappedStringClasses() {
        return extraMappedStringClasses;
    }

    public void setIsIf(boolean isIf) {
        this.isIf = isIf;
    }

    public void clearMappings() {
        mappedStringClass = null;
        mappedStringClasses.clear();
        extraMappedStringClasses.clear();
    }
}
