package com.kj.repo.tt.tool;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.google.common.base.Stopwatch;
import com.kj.repo.infra.tool.TLScriptEngine;

public class TeTLJavaScript {

	private static Invocable js = null;

	static {
		try {
			ScriptEngine engine = TLScriptEngine.jsEngine();
			engine.eval("function say(module){ if(module=='PULL') return (1000 + Math.floor(Math.random()*1000));else return (100 + Math.floor(Math.random()*100)) }");
			js = (Invocable) engine;
		} catch (ScriptException e) {
			e.printStackTrace();
		}

	}

	public static void run(String flag) throws Exception {
		Stopwatch stopwatch = Stopwatch.createStarted();
		int total = 0;
		for (int i = 0; i < 1000000; i++) {
			total += ((Double) js.invokeFunction("say", "PULL")).intValue();
		}
		System.out.println("flag:" + total);
		System.out.println("flag:" + stopwatch.elapsed(TimeUnit.MILLISECONDS));
	}

	public static void run3(String flag) throws NoSuchMethodException, ScriptException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("javascript");
		engine.eval("function say(name){ return Math.random(); }");

		Stopwatch stopwatch = Stopwatch.createStarted();
		int total = 0;
		for (int i = 0; i < 1000000; i++) {
			total += ((Double) ((Invocable) engine).invokeFunction("say", "kj")).intValue();
		}
		System.out.println("flag:" + total);
		System.out.println("flag:" + stopwatch.elapsed(TimeUnit.MILLISECONDS));

	}

	static Random random = new Random();

	public static int javaFun() {
		return 1000 + ThreadLocalRandom.current().nextInt(1000);
	}

	public static void run2(String flag) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		int total = 0;
		for (int i = 0; i < 1000000; i++) {
			total += javaFun();
		}
		System.out.println("flag:" + total);
		System.out.println("flag:" + stopwatch.elapsed(TimeUnit.MILLISECONDS));
	}

	public static void main(String[] args) throws Exception {
		for (int i = 0; i < 100; i++) {
			String f = i + "";
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						TeTLJavaScript.run(f);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}

}
