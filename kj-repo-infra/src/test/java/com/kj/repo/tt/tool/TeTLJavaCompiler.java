package com.kj.repo.tt.tool;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.kj.repo.infra.tool.TLJavaCompiler;

public class TeTLJavaCompiler {

	public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
		StringBuilder java = new StringBuilder();
		java.append("package com.kj;\n") //
				.append("public class CalculatorTest {\n") //
				.append("\tpublic static int multiply(int a, int b) {\n") //
				.append("\tSystem.out.println(a);\n") //
				.append("\tSystem.out.println(b);\n") //
				.append("\treturn a+b;") //
				.append("\t}\n") //
				.append("}\n");
		Class<?> clazz = TLJavaCompiler.compile("com.kj.CalculatorTest", java.toString());
		Method method = clazz.getMethod("multiply", new Class<?>[] { int.class, int.class });
		System.out.println(method.invoke(null, 2, 3));
		System.out.println(System.getProperty("user.dir"));
	}

}
