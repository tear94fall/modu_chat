package com.example.memberservice.lock.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLockProcessorTest {

    private static final String API_LOCK_SOURCE = """
            package com.example.memberservice.global.lock;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import java.util.concurrent.TimeUnit;

            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface ApiLock {
                String prefix() default "";
                TimeUnit timeUnit() default TimeUnit.SECONDS;
                long waitTime() default 15L;
                long leaseTime() default 13L;
            }
            """;

    private static final String LOCK_PARAM_SOURCE = """
            package com.example.memberservice.global.lock;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.PARAMETER)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface LockParam {
            }
            """;

    private static final String LOCKABLE_SOURCE = """
            package com.example.memberservice.global.lock;

            public interface Lockable {
                String getKey();
            }
            """;

    @Test
    void compilesWithStringLockParam(@TempDir Path tempDir) throws IOException {
        String source = """
                package com.example.test;

                import com.example.memberservice.global.lock.ApiLock;
                import com.example.memberservice.global.lock.LockParam;

                public class StringKeyService {
                    @ApiLock
                    public void doWork(@LockParam String key) {
                    }
                }
                """;

        CompileResult result = compile(tempDir, "StringKeyService", source);

        assertNoErrors(result);
    }

    @Test
    void compilesWithLockableLockParam(@TempDir Path tempDir) throws IOException {
        String lockableImpl = """
                package com.example.test;

                import com.example.memberservice.global.lock.Lockable;

                public class MyLockable implements Lockable {
                    @Override
                    public String getKey() {
                        return "key";
                    }
                }
                """;
        String source = """
                package com.example.test;

                import com.example.memberservice.global.lock.ApiLock;
                import com.example.memberservice.global.lock.LockParam;

                public class LockableKeyService {
                    @ApiLock
                    public void doWork(@LockParam MyLockable value) {
                    }
                }
                """;

        CompileResult result = compile(tempDir, "MyLockable", lockableImpl, "LockableKeyService", source);

        assertNoErrors(result);
    }

    @Test
    void failsWhenNoLockParamPresent(@TempDir Path tempDir) throws IOException {
        String source = """
                package com.example.test;

                import com.example.memberservice.global.lock.ApiLock;

                public class NoLockParamService {
                    @ApiLock
                    public void doWork(String key) {
                    }
                }
                """;

        CompileResult result = compile(tempDir, "NoLockParamService", source);

        List<Diagnostic<? extends JavaFileObject>> errors = errorsOf(result);
        assertEquals(1, errors.size(), diagnosticsMessage(result));
        assertTrue(errors.get(0).getMessage(null).contains("@LockParam"));
    }

    @Test
    void failsWhenLockParamOnUnsupportedType(@TempDir Path tempDir) throws IOException {
        String pojo = """
                package com.example.test;

                public class PlainPojo {
                    private String value;
                }
                """;
        String source = """
                package com.example.test;

                import com.example.memberservice.global.lock.ApiLock;
                import com.example.memberservice.global.lock.LockParam;

                public class UnsupportedTypeService {
                    @ApiLock
                    public void doWork(@LockParam PlainPojo pojo) {
                    }
                }
                """;

        CompileResult result = compile(tempDir, "PlainPojo", pojo, "UnsupportedTypeService", source);

        List<Diagnostic<? extends JavaFileObject>> errors = errorsOf(result);
        assertEquals(1, errors.size(), diagnosticsMessage(result));
        assertTrue(errors.get(0).getMessage(null).contains("Lockable"));
    }

    @Test
    void warnsWhenLockParamWithoutApiLock(@TempDir Path tempDir) throws IOException {
        String source = """
                package com.example.test;

                import com.example.memberservice.global.lock.LockParam;

                public class OrphanLockParamService {
                    public void doWork(@LockParam String key) {
                    }
                }
                """;

        CompileResult result = compile(tempDir, "OrphanLockParamService", source);

        assertNoErrors(result);
        boolean hasWarning = result.diagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.WARNING || d.getKind() == Diagnostic.Kind.MANDATORY_WARNING);
        assertTrue(hasWarning, diagnosticsMessage(result));
    }

    private CompileResult compile(Path tempDir, String... nameAndSourcePairs) throws IOException {
        Path srcDir = tempDir.resolve("src");
        Path outDir = tempDir.resolve("out");
        Files.createDirectories(srcDir);
        Files.createDirectories(outDir);

        writeSource(srcDir, "com.example.memberservice.global.lock", "ApiLock", API_LOCK_SOURCE);
        writeSource(srcDir, "com.example.memberservice.global.lock", "LockParam", LOCK_PARAM_SOURCE);
        writeSource(srcDir, "com.example.memberservice.global.lock", "Lockable", LOCKABLE_SOURCE);

        for (int i = 0; i < nameAndSourcePairs.length; i += 2) {
            writeSource(srcDir, "com.example.test", nameAndSourcePairs[i], nameAndSourcePairs[i + 1]);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, List.of(outDir.toFile()));

            List<File> sourceFiles = Files.walk(srcDir)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles);

            List<String> options = new ArrayList<>();
            options.add("-processor");
            options.add("com.example.memberservice.lock.processor.ApiLockProcessor");
            options.add("-classpath");
            options.add(System.getProperty("java.class.path"));
            options.add("-proc:only");

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, options, null, compilationUnits);

            boolean success = task.call();
            return new CompileResult(success, diagnostics.getDiagnostics());
        }
    }

    private void writeSource(Path srcDir, String packageName, String className, String content) throws IOException {
        Path packageDir = srcDir.resolve(packageName.replace('.', '/'));
        Files.createDirectories(packageDir);
        Path file = packageDir.resolve(className + ".java");
        try (Writer writer = Files.newBufferedWriter(file)) {
            writer.write(content);
        }
    }

    private List<Diagnostic<? extends JavaFileObject>> errorsOf(CompileResult result) {
        return result.diagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                .collect(Collectors.toList());
    }

    private void assertNoErrors(CompileResult result) {
        assertTrue(errorsOf(result).isEmpty(), diagnosticsMessage(result));
    }

    private String diagnosticsMessage(CompileResult result) {
        return result.diagnostics().stream()
                .map(d -> d.getKind() + ": " + d.getMessage(null))
                .collect(Collectors.joining("\n"));
    }

    private record CompileResult(boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    }
}
