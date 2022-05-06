package apr.aprlab.repair.search;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.repair.util.ComparatorUtil;
import apr.aprlab.utils.ast.ASTUtil;
import apr.aprlab.utils.ast.ASTWalk;
import apr.aprlab.utils.ast.MyASTParser;
import apr.aprlab.utils.ast.RangeUtil;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.RegexUtil;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.simple.graph.Vertex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchTypeHierarchy {

    public static final Logger logger = LogManager.getLogger(SearchTypeHierarchy.class);

    public static List<String> excludedParentTypes = new ArrayList<String>(Arrays.asList("java."));

    public static List<Set<String>> relativeClasses = new ArrayList<>();

    public static Map<String, CuInfo> classnameToCuInfoMap = new HashMap<>();

    public static Map<String, Map<String, ASTNode>> classnameToFieldMap = new HashMap<>();

    public static Map<String, List<MethodDeclaration>> classnameToMethodMap = new HashMap<>();

    public static Map<String, Float> similarityMap = new HashMap<>();

    public static void clear() {
        relativeClasses.clear();
        classnameToCuInfoMap.clear();
        classnameToFieldMap.clear();
        classnameToMethodMap.clear();
    }

    public static void traverseProject(String srcJavaDir, List<String> depList) {
        List<String> allInterfaces = new ArrayList<String>();
        List<String> allSuperClasses = new ArrayList<String>();
        String saveFile = Paths.get(Globals.outputDir, "allFiles.txt").toString();
        List<File> allFiles = new ArrayList<File>();
        if (new File(saveFile).exists()) {
            List<String> allFileStrs = FileUtil.readFileToList(saveFile);
            for (String fileStr : allFileStrs) {
                allFiles.add(new File(fileStr));
            }
        }
        if (allFiles.isEmpty()) {
            Collection<File> files = FileUtils.listFiles(new File(srcJavaDir), new String[] { "java" }, true);
            allFiles.addAll(files);
            FileUtil.writeToFile(saveFile, allFiles, false);
        }
        logger.debug("number of files in {}: {}", srcJavaDir, allFiles.size());
        for (File file : allFiles) {
            if (Globals.debugMode && shouldSkip(file, Globals.inclusionFilter)) {
                continue;
            }
            String filePath = file.getAbsolutePath();
            char[] sourceCode = FileUtil.readFileToCharArray(filePath);
            CompilationUnit cu = MyASTParser.parseFileToAST(sourceCode, filePath, depList, srcJavaDir);
            List<TypeDeclaration> tdList = new ArrayList<TypeDeclaration>();
            ASTWalk.findall(cu, TypeDeclaration.class, tdList::add);
            String fileClassName = null;
            if (tdList.size() > 1) {
                for (TypeDeclaration td : tdList) {
                    String tdName = String.format("%s.java", td.getName().toString());
                    if (filePath.endsWith(tdName)) {
                        ITypeBinding binding = td.resolveBinding();
                        fileClassName = binding.getQualifiedName();
                        break;
                    }
                }
            }
            if (tdList.isEmpty()) {
                String className = getClassName(null, null, cu, filePath);
                CuInfo cuInfo = new CuInfo(cu, null, filePath, srcJavaDir, className, fileClassName, new String(sourceCode));
                classnameToCuInfoMap.putIfAbsent(className, cuInfo);
            }
            for (TypeDeclaration td : tdList) {
                ITypeBinding binding = td.resolveBinding();
                String className = getClassName(binding, td, cu, filePath);
                if (className.length() == 0) {
                    continue;
                }
                CuInfo cuInfo = new CuInfo(cu, td, filePath, srcJavaDir, className, fileClassName, new String(sourceCode));
                classnameToCuInfoMap.putIfAbsent(className, cuInfo);
                collectFieldsInClass(className, td);
                collectMethodsInClass(className, td);
                if (binding.getSuperclass() != null) {
                    List<Vertex<String>> parentList = new ArrayList<>();
                    List<String> interfaceNameList = ASTUtil.getInterfaceNameList(binding);
                    for (String interfaceName : interfaceNameList) {
                        Vertex<String> parentInterfaceNameType = new Vertex<String>(interfaceName);
                        parentList.add(parentInterfaceNameType);
                    }
                    String superClassName = ASTUtil.getSuperClassName(binding);
                    parentList.add(new Vertex<String>(superClassName));
                    allInterfaces.addAll(interfaceNameList);
                    allSuperClasses.add(superClassName);
                    Vertex<String> childType = new Vertex<String>(className);
                    childType = removeGenericType(childType);
                    for (Vertex<String> parentType : parentList) {
                        if (!parentTypeShouldBeExcluded(parentType)) {
                            parentType = removeGenericType(parentType);
                            addToRelativeClasses(childType, parentType);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < relativeClasses.size(); i++) {
            Set<String> curRelativeClasses = relativeClasses.get(i);
            FileUtil.writeToFile(Globals.runInfoPath, PrintUtil.listToString(curRelativeClasses, String.format("curRelativeClasses size: %s", curRelativeClasses.size())));
        }
        logger.debug(PrintUtil.listToString(relativeClasses, "all relativeClasses"));
    }

    private static void addToRelativeClasses(Vertex<String> childType, Vertex<String> parentType) {
        boolean added = false;
        List<Integer> overlappedIndices = new ArrayList<Integer>();
        for (int i = 0; i < relativeClasses.size(); i++) {
            Set<String> curRelativeClasses = relativeClasses.get(i);
            if (curRelativeClasses.contains(childType.label) || curRelativeClasses.contains(parentType.label)) {
                curRelativeClasses.add(childType.label);
                curRelativeClasses.add(parentType.label);
                added = true;
                overlappedIndices.add(i);
            }
        }
        if (overlappedIndices.size() > 1) {
            List<Set<String>> finalRelClasses = new ArrayList<Set<String>>();
            Set<String> mergedClasses = new HashSet<String>();
            for (int i = 0; i < relativeClasses.size(); i++) {
                Set<String> curRelativeClasses = relativeClasses.get(i);
                if (overlappedIndices.contains(i)) {
                    mergedClasses.addAll(curRelativeClasses);
                } else {
                    finalRelClasses.add(curRelativeClasses);
                }
            }
            finalRelClasses.add(mergedClasses);
            relativeClasses.clear();
            relativeClasses.addAll(finalRelClasses);
        }
        if (!added) {
            Set<String> newRel = new HashSet<String>();
            newRel.add(childType.label);
            newRel.add(parentType.label);
            relativeClasses.add(newRel);
        }
    }

    public static boolean isRelative(String srcCls, String dstCls) {
        for (Set<String> curRelativeClasses : relativeClasses) {
            if (curRelativeClasses.contains(srcCls) && curRelativeClasses.contains(dstCls)) {
                return true;
            }
        }
        return false;
    }

    private static String getClassName(ITypeBinding binding, TypeDeclaration td, CompilationUnit cu, String filePath) {
        String className = null;
        if (binding != null) {
            className = binding.getQualifiedName();
            if (className.length() == 0) {
                logger.warn("The binding fails to get the correct classname: {} (td className: {}, filePath: {})", className, td.getName(), filePath);
                return "";
            }
        }
        PackageDeclaration pd = cu.getPackage();
        if (pd == null) {
            String packageName = getPackageName(filePath);
            logger.warn("current className has null packageDeclaration (from jdt dom API): {}, its real packageName is: {}", className, packageName);
            className = packageName + "." + className;
            logger.warn("the final className (after adding packageName): {}", className);
        }
        if (className == null) {
            String packageName = getPackageName(filePath);
            className = packageName + "." + StringUtil.getFileName(filePath);
        }
        ExceptionUtil.myAssert(className.contains("."));
        return className;
    }

    private static String getPackageName(String filePath) {
        for (String string : FileUtil.readFileToList(filePath)) {
            if (string.trim().startsWith("package ")) {
                List<String> packageNames = RegexUtil.findAll(string, Pattern.compile("package (.*?);"));
                ExceptionUtil.assertNotEmpty(packageNames);
                return packageNames.get(0);
            }
        }
        ExceptionUtil.raise();
        return null;
    }

    @SuppressWarnings("unused")
    private static void debuggingMethodSnippet(CuInfo cuInfo) {
        List<MethodSnippet> msList = new ArrayList<MethodSnippet>(cuInfo.getMethodSnippets());
        msList.sort(ComparatorUtil.methodSnippetComparator);
        for (MethodSnippet ms : msList) {
            Globals.debuggingMsCnt++;
            Globals.donorGraphDir = DirUtil.createNumberedDir(Globals.initGraphDir, "donor_", "");
            logger.debug("debuggingMsCnt: {}, cur ms: {}, donorGraphDir: {}", Globals.debuggingMsCnt, ms, Globals.donorGraphDir);
            ms.contructAndCollect(false);
            cuInfo.getMethodSnippets().remove(ms);
            logger.debug("ms size: {}", cuInfo.getMethodSnippets().size());
        }
    }

    private static boolean parentTypeShouldBeExcluded(Vertex<String> parentType) {
        String type = parentType.label;
        for (String excludeType : excludedParentTypes) {
            if (type.startsWith(excludeType)) {
                return true;
            }
        }
        return false;
    }

    private static void collectMethodsInClass(String className, TypeDeclaration td) {
        ExceptionUtil.assertFalse(classnameToMethodMap.containsKey(className));
        List<MethodDeclaration> methodDeclarations = new ArrayList<>();
        ASTWalk.findall(td, MethodDeclaration.class, methodDeclarations::add);
        classnameToMethodMap.put(className, methodDeclarations);
    }

    private static void collectFieldsInClass(String className, TypeDeclaration td) {
        List<TypeDeclaration> subTds = new ArrayList<TypeDeclaration>();
        ASTWalk.findall(td, TypeDeclaration.class, subTds::add);
        subTds.remove(td);
        ExceptionUtil.assertFalse(classnameToFieldMap.containsKey(className), className);
        Map<String, ASTNode> varToDeclareNodeMap = new HashMap<>();
        List<FieldDeclaration> fieldDeclarations = new ArrayList<>();
        ASTWalk.findall(td, FieldDeclaration.class, fieldDeclarations::add);
        for (FieldDeclaration fd : fieldDeclarations) {
            if (fdIsContainedInSubTds(fd, subTds)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> vdfs = fd.fragments();
            for (VariableDeclarationFragment vdf : vdfs) {
                String varName = vdf.getName().getIdentifier();
                if (varToDeclareNodeMap.containsKey(varName)) {
                    logger.debug("varToDeclareNodeMap.containsKey(varName). varName: {}, className: {}", varName, className);
                }
                varToDeclareNodeMap.put(varName, vdf);
            }
        }
        classnameToFieldMap.put(className, varToDeclareNodeMap);
    }

    private static boolean fdIsContainedInSubTds(FieldDeclaration fd, List<TypeDeclaration> subTds) {
        for (TypeDeclaration subTd : subTds) {
            if (RangeUtil.containsDstNode(subTd, fd)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldSkip(File file, String[] inclusionFilter) {
        if (inclusionFilter.length == 0) {
            return false;
        }
        for (String inclusionString : inclusionFilter) {
            if (file.toString().toLowerCase().contains(inclusionString.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    private static Vertex<String> removeGenericType(Vertex<String> curType) {
        List<String> genericTypeList = RegexUtil.findAll(curType.label, Pattern.compile("<(.*?)>"));
        if (!genericTypeList.isEmpty()) {
            curType = new Vertex<String>(curType.label.split("<")[0]);
            return curType;
        } else {
            return curType;
        }
    }

    public static void addToSimilarityMap(MethodSnippet thisMs, MethodSnippet otherMs, float sim) {
        int thisId = thisMs.getId();
        int otherId = otherMs.getId();
        String key = String.format("%s_%s", thisId, otherId);
        String reverseKey = String.format("%s_%s", otherId, thisId);
        ExceptionUtil.myAssert(!similarityMap.containsKey(key));
        similarityMap.put(key, sim);
        similarityMap.put(reverseKey, sim);
    }

    public static float getResultFromSimilarityMap(MethodSnippet thisMs, MethodSnippet otherMs) {
        int thisId = thisMs.getId();
        int otherId = otherMs.getId();
        String key = String.format("%s_%s", thisId, otherId);
        String reverseKey = String.format("%s_%s", otherId, thisId);
        if (similarityMap.containsKey(key)) {
            return similarityMap.get(key);
        }
        if (similarityMap.containsKey(reverseKey)) {
            return similarityMap.get(reverseKey);
        }
        return -1;
    }

    public static CuInfo getCuInfoByTd(String className, TypeDeclaration td) {
        for (String key : classnameToCuInfoMap.keySet()) {
            if (key.contains(className)) {
                CuInfo cuInfo = classnameToCuInfoMap.get(key);
                if (td == null) {
                    return cuInfo;
                }
                if (td == cuInfo.getTypeDeclaration()) {
                    return cuInfo;
                }
            }
        }
        ExceptionUtil.raise();
        return null;
    }
}
