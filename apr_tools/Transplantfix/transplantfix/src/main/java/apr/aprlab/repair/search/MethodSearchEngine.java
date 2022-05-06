package apr.aprlab.repair.search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import apr.aprlab.repair.config.Globals;
import apr.aprlab.repair.snippet.CodeSnippet;
import apr.aprlab.repair.snippet.MethodSnippet;
import apr.aprlab.utils.general.CollectionUtil;
import apr.aprlab.utils.general.DirUtil;
import apr.aprlab.utils.general.ExceptionUtil;
import apr.aprlab.utils.general.FileUtil;
import apr.aprlab.utils.general.PrintUtil;
import apr.aprlab.utils.general.StringUtil;
import apr.aprlab.utils.general.TimeUtil;
import apr.aprlab.utils.simple.graph.Vertex;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodSearchEngine {

    public static final Logger logger = LogManager.getLogger(MethodSearchEngine.class);

    private MethodSnippet methodSnippet;

    public MethodSearchEngine(MethodSnippet ms) {
        methodSnippet = ms;
    }

    public List<MethodSnippet> search() {
        logger.info("[SEARCH] current snippet: {}", methodSnippet);
        long startTime = System.currentTimeMillis();
        Set<String> relatives = collectSiblings();
        logger.info("time cost of collecting Vertex<String> siblings: {}", TimeUtil.countTime(startTime));
        Globals.siblingsAndParents.clear();
        Globals.siblingsAndParents.addAll(relatives);
        startTime = System.currentTimeMillis();
        List<Vertex<String>> allClasses = collectAllClasses();
        logger.info("time cost of collecting all Vertex<String> classes: {}", TimeUtil.countTime(startTime));
        startTime = System.currentTimeMillis();
        List<MethodSnippet> candMsList = rankingMethodSnippets(allClasses);
        logger.info("time cost of rankingMethodSnippets(): {}", TimeUtil.countTime(startTime));
        logger.debug("candidateMethodSnippetList size (before): {}", candMsList.size());
        if (candMsList.size() > Globals.saveCandidateSnippetCnt) {
            candMsList = candMsList.subList(0, Globals.saveCandidateSnippetCnt);
        }
        FileUtil.writeToFile(Globals.infoPath, String.format("donor methods of current method snippet: %s", methodSnippet));
        FileUtil.writeToFile(Globals.infoPath, PrintUtil.listToString(candMsList));
        if (candMsList.size() > Globals.maxCandidateSnippetCnt) {
            candMsList = candMsList.subList(0, Globals.maxCandidateSnippetCnt);
        }
        logger.debug("candidateMethodSnippetList size (after): {}", candMsList.size());
        int siblingIndex = 0;
        int allIndex = 0;
        for (MethodSnippet ms : candMsList) {
            if (relatives.contains(ms.getClassName())) {
                logger.info("allIndex: {}, siblingIndex: {}, ms: {}", allIndex, siblingIndex, ms);
                FileUtil.writeToFile(Globals.runInfoPath, String.format("allIndex: %s, siblingIndex: %s, ms: %s\n", allIndex, siblingIndex, ms.getMethodSignature()));
                siblingIndex++;
            } else {
                logger.info("allIndex: {}, ms: {}", allIndex, ms);
                FileUtil.writeToFile(Globals.runInfoPath, String.format("allIndex: %s, ms: %s\n", allIndex, ms));
            }
            allIndex++;
        }
        return candMsList;
    }

    private List<Vertex<String>> collectAllClasses() {
        List<Vertex<String>> allClasses = new ArrayList<Vertex<String>>();
        for (String className : SearchTypeHierarchy.classnameToCuInfoMap.keySet()) {
            if (Globals.debugMode) {
                if (Globals.inclusionFilter.length != 0 && !CollectionUtil.containsSameString(Globals.inclusionFilter, StringUtil.getShortName(className))) {
                    continue;
                }
            }
            allClasses.add(new Vertex<String>(className));
        }
        return allClasses;
    }

    private Set<String> collectSiblings() {
        String curClassName = methodSnippet.getClassName();
        for (Set<String> relClasses : SearchTypeHierarchy.relativeClasses) {
            if (relClasses.contains(curClassName)) {
                return relClasses;
            }
        }
        return new HashSet<String>();
    }

    private List<MethodSnippet> rankingMethodSnippets(List<Vertex<String>> classes) {
        List<MethodSnippet> candidateMethodSnippetList = new ArrayList<MethodSnippet>();
        List<TypeDeclaration> visitedTds = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for (Vertex<String> clazz : classes) {
            String fullClassName = clazz.label;
            CuInfo cuInfo = SearchTypeHierarchy.classnameToCuInfoMap.get(fullClassName);
            TypeDeclaration td = cuInfo.getTypeDeclaration();
            if (visitedTds.contains(td)) {
                continue;
            }
            visitedTds.add(td);
            List<MethodSnippet> msList = cuInfo.getMethodSnippets();
            for (MethodSnippet ms : msList) {
                if (methodSnippet == ms) {
                    continue;
                }
                if (Globals.debugMode) {
                    if (!ms.getMethodSignature().toString().contains(Globals.debugDonorSnippet)) {
                        continue;
                    }
                }
                float sim = SearchTypeHierarchy.getResultFromSimilarityMap(methodSnippet, ms);
                if (sim == -1) {
                    sim = methodSnippet.compareSimilarity(ms);
                    SearchTypeHierarchy.addToSimilarityMap(methodSnippet, ms, sim);
                }
                ms.setSimilarity(sim);
                if (!candidateMethodSnippetList.contains(ms)) {
                    candidateMethodSnippetList.add(ms);
                }
            }
        }
        logger.info("time cost of calculating similarity between method snippets: {}", TimeUtil.countTime(startTime));
        startTime = System.currentTimeMillis();
        Collections.sort(candidateMethodSnippetList, new Comparator<CodeSnippet>() {

            @Override
            public int compare(final CodeSnippet cs1, final CodeSnippet cs2) {
                return Float.compare(cs2.getSimilarity(), cs1.getSimilarity());
            }
        });
        logger.info("time cost of ranking candidateMethodSnippetList: {}", TimeUtil.countTime(startTime));
        return candidateMethodSnippetList;
    }

    private void saveRelativeFiles(Set<String> relatives) {
        String siblingDir = Paths.get(Globals.outputDir, "relatives").toString();
        DirUtil.mkdirs(siblingDir);
        for (String sibClassName : relatives) {
            if (SearchTypeHierarchy.classnameToCuInfoMap.get(sibClassName) == null) {
                continue;
            }
            String sibFilePath = SearchTypeHierarchy.classnameToCuInfoMap.get(sibClassName).getFilePath();
            if (new File(sibFilePath).exists()) {
                try {
                    FileUtils.copyFileToDirectory(new File(sibFilePath), new File(siblingDir));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logger.debug("sibFilePath does not exist: {}", sibFilePath);
                ExceptionUtil.raise();
            }
        }
    }
}
