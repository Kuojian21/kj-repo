package com.kj.repo.infra.helper;

import java.io.IOException;

/**
 * @author kj
 */
public class ProcessHelper {

    public static int execSync(String[] command) throws Throwable {

        return RunHelper.call(() -> {
            Process process = exec(command);
            return process.waitFor();
        }, ProcessHelper.class.getName(), "execSync", (Object) command);
    }

    public static Process exec(String[] command) throws IOException {
        return Runtime.getRuntime().exec(command);
    }

}
