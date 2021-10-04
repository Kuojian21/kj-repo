package com.kj.repo.demo.debug;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/compact3-package-summary.html
 * -javaagent:jarpath[=options]
 *
 * @author kj
 */
public class Agent {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                System.out.println("premain:" + className);
                return classfileBuffer;
            }
        }, true);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                System.out.println("agentmain:" + className);
                return classfileBuffer;
            }
        }, true);
    }

}
