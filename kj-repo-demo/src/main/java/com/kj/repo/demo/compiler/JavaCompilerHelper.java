package com.kj.repo.demo.compiler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;

import com.kj.repo.infra.logger.LoggerHelper;
import com.kj.repo.infra.utils.RunUtil;

/**
 * @author kj
 */
public class JavaCompilerHelper {

    private static final Logger logger = LoggerHelper.getLogger();
    private static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private static StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    private static URLClassLoader classLoader;
    private static File out;

    static {
        try {
            out = new File(System.getProperty("user.dir") + File.separator + "target/compile");
            if (!out.exists()) {
                out.mkdirs();
            }
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(out));
            classLoader = new URLClassLoader(new URL[] {out.toURI().toURL()});
        } catch (IOException e) {
            logger.error("", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    fileManager.close();
                } catch (IOException e) {
                    logger.error("", e);
                }
            }
        });
    }

    public static synchronized Class<?> compile(String name, String body) {
        return RunUtil.run(() -> compileInternal(name, body),
                JavaCompilerHelper.class.getName(), "compileInternal", name);
    }

    public static synchronized Class<?> compileInternal(String name, String body)
            throws IOException, ClassNotFoundException {
        JavaCompiler.CompilationTask task = compiler
                .getTask(null, fileManager, null, null, null,
                        Arrays.asList(new SimpleJavaFileObject(
                                URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
                                Kind.SOURCE) {
                            @Override
                            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                                return body;
                            }
                        }));
        fileManager.flush();
        if (task.call()) {
            return classLoader.loadClass(name);
        }
        return null;
    }

}
