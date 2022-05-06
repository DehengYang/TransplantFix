package apr.aprlab.repair.adapt.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import apr.aprlab.utils.graph.ddg.MethodCall;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityInMethod {

    public static final Logger logger = LogManager.getLogger(EntityInMethod.class);

    private List<VariableName> varNames = new ArrayList<VariableName>();

    private List<TypeName> typeNames = new ArrayList<TypeName>();

    private List<MethodName> methodNames = new ArrayList<MethodName>();

    private String simpleClassName;

    public Map<String, String> classSubNameMapping = new HashMap<String, String>();

    public Map<String, String> typeMapping = new HashMap<String, String>();

    public Map<String, String> methodMapping = new HashMap<String, String>();

    public Map<String, String> methodCallMapping = new HashMap<String, String>();

    public Map<String, List<String>> varMapping = new HashMap<>();

    private List<Entity> allEntities = new ArrayList<Entity>();

    public void clearMappings() {
        classSubNameMapping.clear();
        typeMapping.clear();
        methodCallMapping.clear();
        methodMapping.clear();
        varMapping.clear();
    }

    public EntityInMethod(String simpleClassName) {
        this.simpleClassName = simpleClassName;
    }

    public void addVar(String varName, SimpleName varNode, VariableDef varDef) {
        Entity matchedVarName = matchedEntity(varName, new ArrayList<Entity>(varNames));
        if (matchedVarName != null) {
            ((VariableName) matchedVarName).addVar(varNode, varDef);
        } else {
            VariableName newEntity = new VariableName(varNode, varDef);
            varNames.add(newEntity);
            allEntities.add(newEntity);
        }
    }

    public void addType(String typeName, ASTNode typeNode) {
        Entity matchedTypeName = matchedEntity(typeName, new ArrayList<Entity>(typeNames));
        if (matchedTypeName != null) {
            ((TypeName) matchedTypeName).addType(typeNode);
        } else {
            TypeName newEntity = new TypeName(typeName, typeNode);
            typeNames.add(newEntity);
            allEntities.add(newEntity);
        }
    }

    public void addAPI(ASTNode methodSimpleName, ASTNode wholeMethodNode, MethodCall mc) {
        Entity matchedMethodName = matchedEntity(mc.getMethodCallSignature(), new ArrayList<Entity>(methodNames));
        if (matchedMethodName != null) {
            ((MethodName) matchedMethodName).addMethod(methodSimpleName, wholeMethodNode);
        } else {
            MethodName newEntity = new MethodName(methodSimpleName, wholeMethodNode, mc);
            methodNames.add(newEntity);
            allEntities.add(newEntity);
        }
    }

    private Entity matchedEntity(String signature, List<Entity> entities) {
        for (Entity mapping : entities) {
            if (mapping.getSignature().equals(signature)) {
                return mapping;
            }
        }
        return null;
    }

    public List<Entity> getEntitiesInASTNode(ASTNode astNode) {
        List<Entity> matchedEntities = new ArrayList<>();
        for (Entity cur : allEntities) {
            Entity entity = cur.generateEntityInASTNode(astNode);
            if (entity != null) {
                matchedEntities.add(entity);
            }
        }
        return matchedEntities;
    }

    public List<VariableName> getVarNames() {
        return varNames;
    }

    public List<TypeName> getTypeNames() {
        return typeNames;
    }

    public List<MethodName> getMethodNames() {
        return methodNames;
    }

    public List<Entity> getAllEntities() {
        return allEntities;
    }

    public void getMappings(EntityInMethod srcEntityInMethod) {
        MappingUtil.getSubNameMapping(simpleClassName, srcEntityInMethod.getSimpleClassName(), classSubNameMapping);
        MappingUtil.typeMapping(typeNames, srcEntityInMethod.getTypeNames(), typeMapping, classSubNameMapping);
        if (!getSimpleClassName().equals(srcEntityInMethod.getSimpleClassName())) {
            typeMapping.putIfAbsent(getSimpleClassName(), srcEntityInMethod.getSimpleClassName());
        }
        MappingUtil.methodMapping(methodNames, srcEntityInMethod.getMethodNames(), methodMapping, methodCallMapping, classSubNameMapping, typeMapping);
        MappingUtil.varMapping(varNames, srcEntityInMethod.getVarNames(), varMapping, typeMapping);
    }

    public String getSimpleClassName() {
        return simpleClassName;
    }

    public Map<String, String> getClassSubNameMapping() {
        return classSubNameMapping;
    }

    public Map<String, String> getTypeMapping() {
        return typeMapping;
    }

    public Map<String, String> getMethodMapping() {
        return methodMapping;
    }

    public Map<String, List<String>> getVarMapping() {
        return varMapping;
    }

    public List<String> getVarNameStrings() {
        List<String> varNameStrings = new ArrayList<String>();
        for (VariableName vn : varNames) {
            varNameStrings.add(vn.getVarName());
        }
        return varNameStrings;
    }

    public Map<String, String> getMethodCallMapping() {
        return methodCallMapping;
    }
}
