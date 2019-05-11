package com.kj.repo.infra.tool;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author kuojian21
 */
public class TLScriptEngine {

    public static final ScriptEngineManager manager = new ScriptEngineManager();

    public static void main(String[] args) {
        for (ScriptEngineFactory factory : manager.getEngineFactories()) {
            System.out.println("supported script engine:" + factory.getNames());
        }
    }

    public static ScriptEngine jsEngine() throws ScriptException {
        return manager.getEngineByName("javascript");
    }

    public static ScriptEngine ecmaEngine() throws ScriptException {
        return manager.getEngineByName("ecmascript");
    }

    public static ScriptEngine nashornEngine() throws ScriptException {
        return manager.getEngineByName("nashorn");
    }

    public static ScriptEngine luaEngine() throws ScriptException {
        return manager.getEngineByName("lua");
    }

}
