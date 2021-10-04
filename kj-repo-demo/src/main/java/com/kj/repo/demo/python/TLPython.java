package com.kj.repo.demo.python;

import org.python.util.PythonInterpreter;

/**
 * @author kj
 */
public class TLPython {

    public static PythonInterpreter interpreter() {
        return new PythonInterpreter();
    }

    public static void exec(String expr) {
        PythonInterpreter interpreter = interpreter();
        interpreter.exec(expr);
    }

    public static void main(String[] args) {
        exec("a=[5,2,3,9,4,0]; ");
        exec("print(sorted(a));");
    }

}
