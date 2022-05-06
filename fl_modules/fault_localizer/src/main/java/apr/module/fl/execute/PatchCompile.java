package apr.module.fl.execute;

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

public class PatchCompile {

    List<String> compilerOpts = new ArrayList<>();

    public PatchCompile(List<String> dependencies) {
        this(dependencies, "1.7");
    }

    public PatchCompile(List<String> dependencies, String compileLevel) {
        compilerOpts.add("-nowarn");
        compilerOpts.add("-source");
        compilerOpts.add(compileLevel);
        compilerOpts.add("-cp");
        String depStr = "";
        for (String dep : dependencies) {
            depStr += File.pathSeparator + dep;
        }
        compilerOpts.add(depStr);
    }

    public void setOutputPath(String outputPath) {
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
        final CompilationTask task = compiler.getTask(null, manager, diagnostics, this.compilerOpts, null, compilationUnits);
        Boolean result = task.call();
        for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            System.out.format("%s, line %d in %s", diagnostic.getMessage(null), diagnostic.getLineNumber(), diagnostic.getSource().getName());
        }
        if (result == null || !result) {
            System.out.println("\nCompilation failed. File path: " + filePath);
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
