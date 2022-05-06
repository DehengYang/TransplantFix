package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.ASTNode;
import apr.aprlab.repair.search.SearchTypeHierarchy;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.Pair;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.RegexUtil;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.graph.ddg.MethodCall;
import apr.aprlab.utils.similarity.SimilarityUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MappingUtil {

    public static final Logger logger = LogManager.getLogger(MappingUtil.class);

    public static String applyMappings(List<Pair<ASTNode, String>> mappings, ASTNode astNode, String fileString) {
        String mappedString = "";
        int startPos = astNode.getStartPosition();
        int endPos = astNode.getStartPosition() + astNode.getLength();
        for (Pair<ASTNode, String> pair : mappings) {
            ASTNode node = pair.getLeft();
            if (RangeUtil.containsDstNode(astNode, node)) {
                int cutStartPos = node.getStartPosition();
                int cutEndPos = node.getStartPosition() + node.getLength();
                if (startPos > cutStartPos) {
                    logger.debug(PrintUtil.listToString(mappings));
                    ExceptionUtil.raise();
                }
                mappedString += StringUtil.substring(fileString, startPos, cutStartPos);
                mappedString += pair.getRight();
                startPos = cutEndPos;
            }
        }
        mappedString += StringUtil.substring(fileString, startPos, endPos);
        return mappedString;
    }

    public static String applySingleMapping(Pair<ASTNode, String> mapping, ASTNode astNode, String fileString) {
        String mappedString = "";
        int startPos = astNode.getStartPosition();
        int endPos = astNode.getStartPosition() + astNode.getLength();
        ASTNode node = mapping.getLeft();
        int cutStartPos = node.getStartPosition();
        int cutEndPos = node.getStartPosition() + node.getLength();
        mappedString += StringUtil.substring(fileString, startPos, cutStartPos);
        mappedString += mapping.getRight();
        startPos = cutEndPos;
        mappedString += StringUtil.substring(fileString, startPos, endPos);
        return mappedString;
    }

    public static void varMapping(List<VariableName> dstVarNames, List<VariableName> srcVarNames, Map<String, List<String>> varMapping, Map<String, String> typeMapping) {
        for (VariableName dstVn : dstVarNames) {
            String dstVarName = dstVn.getVarName();
            List<Pair<String, Float>> matched = new ArrayList<>();
            for (VariableName srcVn : srcVarNames) {
                String srcVarName = srcVn.getVarName();
                if (varMatched(srcVn, dstVn, typeMapping)) {
                    float curSim = SimilarityUtil.compare(srcVarName, dstVarName);
                    matched.add(new Pair<>(srcVarName, curSim));
                }
            }
            matched.sort(new Comparator<Pair<String, Float>>() {

                @Override
                public int compare(Pair<String, Float> o1, Pair<String, Float> o2) {
                    return Float.compare(o2.getRight(), o1.getRight());
                }
            });
            List<String> matchedStrings = new ArrayList<String>();
            for (Pair<String, Float> pair : matched) {
                matchedStrings.add(pair.getLeft());
            }
            if (!matchedStrings.isEmpty()) {
                varMapping.put(dstVarName, matchedStrings);
            }
        }
    }

    private static boolean varMatched(VariableName srcVariable, VariableName dstVariable, Map<String, String> typeMapping) {
        String srcType = srcVariable.getType();
        String dstType = dstVariable.getType();
        if (srcType.equals(dstType)) {
            return true;
        } else {
            if (typeMapping.containsKey(dstType) && typeMapping.get(dstType).equals(srcType)) {
                return true;
            }
        }
        return false;
    }

    public static void methodMapping(List<MethodName> dstMethodNames, List<MethodName> srcMethodNames, Map<String, String> methodMapping, Map<String, String> methodCallMapping, Map<String, String> classSubNameMapping, Map<String, String> typeMapping) {
        for (MethodName dstMn : dstMethodNames) {
            MethodCall dstMc = dstMn.getMethodCall();
            if (dstMc.toString().contains("init")) {
                logger.debug("");
            }
            MethodName bestFit = null;
            float bestSim = -1;
            for (MethodName srcMs : srcMethodNames) {
                MethodCall srcMc = srcMs.getMethodCall();
                float methodNameSim = SimilarityUtil.getJaccardSimilarity(dstMc.getMethodName(), srcMc.getMethodName());
                List<String> dstParas = getSplitParas(dstMc);
                List<String> srcParas = getSplitParas(srcMc);
                float paraSim = SimilarityUtil.getJaccardSimilarity(dstParas, srcParas);
                float clsSim = 0.0f;
                float pkgSim = 0.0f;
                String dstCls = dstMc.getClassType();
                String srcCls = srcMc.getClassType();
                if (SearchTypeHierarchy.isRelative(dstCls, srcCls)) {
                    clsSim = 1.0f;
                    pkgSim = 1.0f;
                } else {
                    String dstClassName = dstCls.substring(dstCls.lastIndexOf(".") + 1);
                    if (typeMapping.containsKey(dstClassName)) {
                        dstClassName = typeMapping.get(dstClassName);
                    }
                    if (dstCls.contains(".") && srcCls.contains(".")) {
                        clsSim = SimilarityUtil.getJaccardSimilarity(dstCls.substring(dstCls.lastIndexOf(".") + 1), srcCls.substring(srcCls.lastIndexOf(".") + 1));
                        pkgSim = SimilarityUtil.getJaccardSimilarity(dstCls.substring(0, dstCls.lastIndexOf(".")), srcCls.substring(0, srcCls.lastIndexOf(".")));
                    }
                }
                float finalSim = methodNameSim + paraSim + clsSim + pkgSim;
                finalSim = finalSim / 4;
                if (bestSim < finalSim) {
                    bestSim = finalSim;
                    bestFit = srcMs;
                }
            }
            if (bestSim >= 0.6) {
                methodCallMapping.put(dstMn.getMethodName(), bestFit.getWholeNodes().get(0).toString());
            }
        }
        for (MethodName dstMn : dstMethodNames) {
            String dstMethodName = dstMn.getMethodName();
            String mappedString = containsClassUniqueName(dstMethodName, classSubNameMapping);
            if (!mappedString.equals(dstMethodName)) {
                methodMapping.put(dstMethodName, mappedString);
            }
        }
    }

    private static List<String> getSplitParas(MethodCall dstMc) {
        List<String> dstParas = new ArrayList<String>();
        for (String para : dstMc.getParameterTypes()) {
            dstParas.addAll(RegexUtil.splitCamelCase(para));
        }
        return dstParas;
    }

    public static void typeMapping(List<TypeName> dstTypeNames, List<TypeName> srcTypeNames, Map<String, String> typeMapping, Map<String, String> classSubNameMapping) {
        for (TypeName dstTn : dstTypeNames) {
            String dstTypeName = dstTn.getTypeName();
            String mappedString = containsClassUniqueName(dstTypeName, classSubNameMapping);
            if (!mappedString.equals(dstTypeName)) {
                typeMapping.put(dstTypeName, mappedString);
            }
        }
    }

    private static String containsClassUniqueName(String dstTypeName, Map<String, String> classSubNameMapping) {
        for (String key : classSubNameMapping.keySet()) {
            if (dstTypeName.contains(key)) {
                dstTypeName = dstTypeName.replace(key, classSubNameMapping.get(key));
            }
        }
        return dstTypeName;
    }

    public static void getSubNameMapping(String dstClassName, String srcClassName, Map<String, String> classSubNameMapping) {
        List<String> srcNameSplits = RegexUtil.splitCamelCase(srcClassName);
        List<String> dstNameSplits = RegexUtil.splitCamelCase(dstClassName);
        List<String> commonNames = new ArrayList<String>();
        for (String dstSplit : dstNameSplits) {
            if (srcNameSplits.contains(dstSplit)) {
                commonNames.add(dstSplit);
            }
        }
        srcClassName = removeCommonNames(srcNameSplits, commonNames);
        dstClassName = removeCommonNames(dstNameSplits, commonNames);
        classSubNameMapping.put(dstClassName, srcClassName);
    }

    private static String removeCommonNames(List<String> srcNameSplits, List<String> commonNames) {
        String newString = "";
        for (String split : srcNameSplits) {
            if (!commonNames.contains(split)) {
                newString += split;
            }
        }
        return newString;
    }

    public static int getMaxMappingCntForVar(List<VariableName> vars, Map<String, List<String>> varMapping) {
        int maxMappingCnt = 1;
        for (VariableName var : vars) {
            if (varMapping.containsKey(var.getVarName())) {
                maxMappingCnt = Math.max(maxMappingCnt, varMapping.get(var.getVarName()).size());
            }
        }
        return maxMappingCnt;
    }

    public static boolean mappingConflictsOrVarNotExists(MappedStringClass mappedStringClass, List<MappedStringClass> usedMappedStringClasses, MethodSnippet srcMethodSnippet) {
        boolean mappingConflicts = mappingConflicts(mappedStringClass, usedMappedStringClasses);
        boolean varNotExists = varNotExists(mappedStringClass, usedMappedStringClasses, srcMethodSnippet);
        return mappingConflicts || varNotExists;
    }

    private static boolean varNotExists(MappedStringClass mappedStringClass, List<MappedStringClass> usedMappedStringClasses, MethodSnippet srcMethodSnippet) {
        MyUnit myUnit = mappedStringClass.getMyUnit();
        if (myUnit == null) {
            return false;
        }
        List<VariableName> vars = myUnit.getVars();
        Map<String, String> varMapping = mappedStringClass.getVarMapping();
        if (varMapping == null) {
            return false;
        }
        List<VariableName> unmappedVars = new ArrayList<VariableName>();
        for (VariableName var : vars) {
            if (!varMapping.containsKey(var.getVarName())) {
                unmappedVars.add(var);
            }
        }
        if (unmappedVars.isEmpty()) {
            return false;
        }
        for (VariableName var : unmappedVars) {
            List<VariableDef> varDefs = var.getVarDefs();
            List<MappedStringClass> mappedStringClasses = new ArrayList<MappedStringClass>(usedMappedStringClasses);
            mappedStringClasses.add(mappedStringClass);
            if (containsVarDefs(mappedStringClasses, varDefs)) {
                return false;
            } else if (srcMethodSnippetContainsVar(srcMethodSnippet, var.getVarName())) {
                return false;
            }
        }
        return true;
    }

    private static boolean srcMethodSnippetContainsVar(MethodSnippet srcMethodSnippet, String varName) {
        Map<String, ASTNode> fieldVarMap = SearchTypeHierarchy.classnameToFieldMap.get(srcMethodSnippet.getClassName());
        boolean containedInFieldVar = fieldVarMap.containsKey(varName);
        Map<String, List<VariableDef>> varDefMap = srcMethodSnippet.getVarDefMap();
        boolean containedInMethodVars = varDefMap.containsKey(varName);
        return containedInFieldVar || containedInMethodVars;
    }

    private static boolean containsVarDefs(List<MappedStringClass> usedMappedStringClasses, List<VariableDef> varDefs) {
        for (VariableDef varDef : varDefs) {
            if (containsVarDef(usedMappedStringClasses, varDef)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsVarDef(List<MappedStringClass> usedMappedStringClasses, VariableDef varDef) {
        for (MappedStringClass usedClass : usedMappedStringClasses) {
            MyUnit usedMyUnit = usedClass.getMyUnit();
            if (usedMyUnit == null) {
                continue;
            }
            ASTNode usedAstNode = usedMyUnit.getAstNode();
            if (RangeUtil.containsDstNode(usedAstNode, varDef.getDefNode())) {
                return true;
            }
        }
        return false;
    }

    public static boolean mappingConflicts(MappedStringClass mappedStringClass, List<MappedStringClass> usedMappedStringClasses) {
        for (MappedStringClass curClass : usedMappedStringClasses) {
            if (curClass.isConflict(mappedStringClass)) {
                return true;
            }
        }
        return false;
    }

    public static MappedStringClass findNonConflictMappedStringClass(List<MappedStringClass> mappedStringClasses, List<MappedStringClass> usedMappedStringClasses, MethodSnippet srcMethodSnippet) {
        for (MappedStringClass curClass : mappedStringClasses) {
            if (!mappingConflictsOrVarNotExists(curClass, usedMappedStringClasses, srcMethodSnippet)) {
                return curClass;
            }
        }
        return null;
    }
}
