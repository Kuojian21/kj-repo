package com.kj.repo.infra.helper;

import java.io.IOException;

/**
 * @author kuojian21
 */
public class ProcessHelper {

    public static int execSync(String[] command) throws IOException, InterruptedException {
        Process process = exec(command);
        return process.waitFor();
    }

    public static Process exec(String[] command) throws IOException {
        return Runtime.getRuntime().exec(command);
    }

}
