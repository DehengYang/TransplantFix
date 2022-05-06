package apr.junit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import apr.junit.utils.FileUtil;

public class PatchCompile {

    List<String> compilerOpts = new ArrayList<>();

    private String jvmPath;

    private String compileLevel;

    private List<String> deps;

    private String outputDirPath;

    private String logFilePath;

    private boolean cleanCompileOutDir;

    public PatchCompile(String logFilePath, List<String> dependencies, String jvmPath, String outputDirPath, boolean cleanCompileOutDir) {
        this.deps = dependencies;
        this.jvmPath = jvmPath;
        if (this.jvmPath.contains("jdk1.8")) {
            compileLevel = "1.8";
        } else {
            compileLevel = "1.7";
        }
        this.outputDirPath = outputDirPath;
        this.logFilePath = logFilePath;
        this.cleanCompileOutDir = cleanCompileOutDir;
        init();
    }

    public void init() {
        compilerOpts.add("-nowarn");
        compilerOpts.add("-source");
        compilerOpts.add(compileLevel);
        compilerOpts.add("-target");
        compilerOpts.add(compileLevel);
        compilerOpts.add("-cp");
        String depStr = "";
        for (String dep : deps) {
            depStr += File.pathSeparator + dep;
        }
        compilerOpts.add(depStr);
        setOutputPath(this.outputDirPath);
    }

    public void setOutputPath(String outputPath) {
        File dir = new File(outputPath);
        if (dir.exists() && cleanCompileOutDir) {
            try {
                FileUtils.deleteDirectory(new File(outputPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        compilerOpts.add("-d");
        compilerOpts.add(outputPath);
    }

    public Boolean compilePatchFile(String filePath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find the system Java compiler. Please check that your class path includes tools.jar");
        }
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null);
        final File file = new File(filePath);
        final Iterable<? extends JavaFileObject> compilationUnits = manager.getJavaFileObjectsFromFiles(Arrays.asList(file));
        final CompilationTask task = compiler.getTask(null, manager, diagnostics, compilerOpts, null, compilationUnits);
        Boolean result = task.call();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            String error = String.format("%s (at line %d)\n", diagnostic.getMessage(null), diagnostic.getLineNumber());
            FileUtil.writeToFile(logFilePath, error);
        }
        if (result == null || !result) {
            System.out.println("Compilation failed. File path: " + filePath);
        } else {
            System.out.println("Compilation passed. File path: " + filePath);
        }
        try {
            manager.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
