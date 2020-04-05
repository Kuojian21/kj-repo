package com.kj.repo.benchmark.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ResultRole;
import org.openjdk.jmh.results.ThroughputResult;
import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.TempFile;
import org.openjdk.jmh.util.Utils;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

/**
 * @author kj
 */
public class DTraceSystemCallProfiler implements ExternalProfiler {
    private Stopwatch stopwatch;
    private Process dtraceProcess;
    private TempFile perfBinData;

    public static void main(String[] args) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("/Users/kj/s.txt"));
        for (String line : lines) {
            System.out.print("bg_");
            for (String cell : line.trim().split("\\s+")) {
                System.out.print(cell + " ");
            }
            System.out.println("ed");
        }
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Lists.newArrayList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        return Lists.newArrayList();
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {
        try {
            perfBinData = FileUtils.weakTempFile("perfbin");
            stopwatch = Stopwatch.createStarted();
            dtraceProcess =
                    Utils.runAsync("sudo", "dtrace", "-n", "syscall:::entry { @num[pid,execname] = count(); }", "-o",
                            perfBinData.getAbsolutePath());
        } catch (IOException e) {

        }
    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        try {
            long elapsed = stopwatch.elapsed(TimeUnit.NANOSECONDS);
            dtraceProcess.destroyForcibly();
            dtraceProcess.waitFor();
            List<String> lines = Files.readAllLines(perfBinData.file().toPath());
            for (String line : lines) {
                if (line.contains(pid + "")) {
                    String[] cells = line.trim().split("\\s+");
                    if (Long.parseLong(cells[0]) == pid) {
                        return Lists.newArrayList(
                                new ThroughputResult(ResultRole.SECONDARY, "system::call", Double.parseDouble(cells[2]),
                                        elapsed, TimeUnit.SECONDS));
                    }
                }
            }
        } catch (Exception e) {

        } finally {
            perfBinData.delete();
        }
        return Lists.newArrayList();
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public String getDescription() {
        return getClass().getName();
    }
}
