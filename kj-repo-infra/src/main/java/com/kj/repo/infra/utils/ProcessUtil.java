package com.kj.repo.infra.utils;

import java.io.IOException;

/**
 * @author kj
 */
public class ProcessUtil {

    public static int execSync(String[] command) throws Throwable {

        return RunUtil.call(() -> {
            Process process = exec(command);
            return process.waitFor();
        }, ProcessUtil.class.getName(), "execSync", (Object) command);
    }

    public static Process exec(String[] command) throws IOException {
        return Runtime.getRuntime().exec(command);
    }

}
