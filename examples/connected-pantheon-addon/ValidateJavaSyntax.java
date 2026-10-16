import com.sun.source.util.JavacTask;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** Parse only: no Minecraft dependencies, type checking, class output or Gradle invocation. */
public final class ValidateJavaSyntax {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null || args.length == 0) throw new IllegalArgumentException("Use a JDK and supply Java source paths");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, null, null)) {
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                List.of("--release", "17", "-proc:none"), null, files.getJavaFileObjects(args));
            for (var ignored : task.parse()) { /* Force parsing of every supplied compilation unit. */ }
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) throw new IllegalStateException(diagnostic.toString());
            }
        }
        System.out.println("Java 17 syntax parsed: " + args.length + " files (not compiled or type-checked)");
    }
}
