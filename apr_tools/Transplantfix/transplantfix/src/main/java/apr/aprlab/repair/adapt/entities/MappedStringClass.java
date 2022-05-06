package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.ASTNode;
import apr.aprlab.repair.adapt.ged.util.Node;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MappedStringClass {

    public static final Logger logger = LogManager.getLogger(MappedStringClass.class);

    private Map<String, String> typeMapping;

    private Map<String, String> methodMapping;

    private Map<String, String> varMapping;

    private MyUnit myUnit;

    private List<Pair<ASTNode, String>> mappings = new ArrayList<>();

    private String mappedString;

    private boolean isExtraMappedString = false;

    private Node extraMappedNode;

    public MappedStringClass(Map<String, String> typeMapping, Map<String, String> methodMapping, Map<String, List<String>> varMapping, int curMappingCnt, MyUnit myUnit) {
        this.typeMapping = typeMapping;
        this.methodMapping = methodMapping;
        this.myUnit = myUnit;
        for (TypeName type : myUnit.getTypes()) {
            if (typeMapping.containsKey(type.getTypeName())) {
                for (ASTNode node : type.getNodes()) {
                    mappings.add(new Pair<>(node, typeMapping.get(type.getTypeName())));
                }
            }
        }
        for (MethodName method : myUnit.getMethods()) {
            boolean changed = false;
            if (method.getMethodCall().isStaticInvoke()) {
                for (ASTNode node : method.getNodes()) {
                    ASTNode parent = node.getParent();
                    if (!parent.toString().contains("." + method.getMethodName() + "(")) {
                        String staticCall = method.getMethodCall().getClassType() + "." + method.getMethodName();
                        changed = true;
                        mappings.add(new Pair<>(node, staticCall));
                    }
                }
            }
            if (!changed) {
                if (methodMapping.containsKey(method.getMethodName())) {
                    for (ASTNode node : method.getNodes()) {
                        mappings.add(new Pair<>(node, methodMapping.get(method.getMethodName())));
                    }
                }
            }
        }
        Map<String, String> curVarMapping = new HashMap<String, String>();
        List<String> usedOriVarStrs = new ArrayList<>();
        for (VariableName var : myUnit.getVars()) {
            if (varMapping.containsKey(var.getVarName())) {
                for (ASTNode node : var.getNodes()) {
                    String varName = var.getVarName();
                    String mappedString = obtainMappedStringByCnt(varName, curMappingCnt, varMapping);
                    if (!curVarMapping.containsKey(varName)) {
                        while (usedOriVarStrs.contains(mappedString) && curMappingCnt < varMapping.get(varName).size()) {
                            curMappingCnt++;
                            mappedString = obtainMappedStringByCnt(varName, curMappingCnt, varMapping);
                        }
                        curVarMapping.put(varName, mappedString);
                        usedOriVarStrs.add(mappedString);
                    }
                    mappings.add(new Pair<>(node, mappedString));
                }
            }
        }
        this.varMapping = curVarMapping;
        mappings.sort(ComparatorUtil.astNodePairComparator);
        mappedString = MappingUtil.applyMappings(mappings, myUnit.getAstNode(), myUnit.getMethodSnippet().getFileString());
    }

    private String obtainMappedStringByCnt(String varName, int curMappingCnt, Map<String, List<String>> varMapping) {
        String mappedString = varMapping.get(varName).get(0);
        if (curMappingCnt < varMapping.get(varName).size()) {
            mappedString = varMapping.get(varName).get(curMappingCnt);
        }
        return mappedString;
    }

    public MappedStringClass(String mappedString) {
        this.mappedString = mappedString;
    }

    public MappedStringClass(Map<String, String> typeMapping, Map<String, String> methodMapping, Map<String, String> varMapping, String mappedString, MyUnit myUnit) {
        this.typeMapping = typeMapping;
        this.methodMapping = methodMapping;
        this.varMapping = varMapping;
        this.mappedString = mappedString;
        this.myUnit = myUnit;
    }

    public MappedStringClass(List<Pair<ASTNode, String>> oldMappings, MyUnit myUnit, Map<String, String> theVarMapping) {
        this.mappings = oldMappings;
        this.myUnit = myUnit;
        this.varMapping = theVarMapping;
        mappings.sort(ComparatorUtil.astNodePairComparator);
        mappedString = MappingUtil.applyMappings(mappings, myUnit.getAstNode(), myUnit.getMethodSnippet().getFileString());
    }

    public Map<String, String> getTypeMapping() {
        return typeMapping;
    }

    public Map<String, String> getMethodMapping() {
        return methodMapping;
    }

    public Map<String, String> getVarMapping() {
        return varMapping;
    }

    public List<Pair<ASTNode, String>> getMappings() {
        return mappings;
    }

    public String getMappedString() {
        return mappedString;
    }

    public void setExtraMappedNode(Node startNode) {
        isExtraMappedString = true;
        this.extraMappedNode = startNode;
    }

    public boolean isExtraMappedString() {
        return isExtraMappedString;
    }

    public boolean isConflict(MappedStringClass otherClass) {
        Map<String, String> otherVarMapping = otherClass.getVarMapping();
        if (varMapping == null || otherVarMapping == null) {
            return false;
        }
        List<String> commonKeys = CollectionUtil.getIntersection(varMapping.keySet(), otherVarMapping.keySet());
        for (String key : commonKeys) {
            if (!varMapping.get(key).equals(otherVarMapping.get(key))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("mappedString: %s", mappedString);
    }

    public Node getExtraMappedNode() {
        return extraMappedNode;
    }

    public MyUnit getMyUnit() {
        return myUnit;
    }
}
