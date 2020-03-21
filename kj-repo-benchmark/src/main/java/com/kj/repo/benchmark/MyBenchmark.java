/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.kj.repo.benchmark;

import org.openjdk.jmh.profile.ClassloaderProfiler;
import org.openjdk.jmh.profile.CompilerProfiler;
import org.openjdk.jmh.profile.DTraceAsmProfiler;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.HotspotClassloadingProfiler;
import org.openjdk.jmh.profile.HotspotCompilationProfiler;
import org.openjdk.jmh.profile.HotspotMemoryProfiler;
import org.openjdk.jmh.profile.HotspotRuntimeProfiler;
import org.openjdk.jmh.profile.HotspotThreadProfiler;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
import org.openjdk.jmh.profile.LinuxPerfProfiler;
import org.openjdk.jmh.profile.PausesProfiler;
import org.openjdk.jmh.profile.SafepointsProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.profile.WinPerfAsmProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

import com.kj.repo.benchmark.crypt.BenchmarkCrypt;

public class MyBenchmark {
    org.openjdk.jmh.runner.options.Options option;

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                .include(BenchmarkCrypt.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupBatchSize(1)
                .warmupTime(TimeValue.seconds(3))
                .warmupMode(WarmupMode.BULK)
                .measurementIterations(3)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(30))
                .threads(Runtime.getRuntime().availableProcessors())
                .timeout(TimeValue.seconds(10))
                .syncIterations(true)
                .addProfiler(DTraceAsmProfiler.class)
                .addProfiler(LinuxPerfAsmProfiler.class)
                .addProfiler(WinPerfAsmProfiler.class)
                .addProfiler(LinuxPerfNormProfiler.class)
                .addProfiler(SafepointsProfiler.class)
                .addProfiler(LinuxPerfProfiler.class)
                .addProfiler(GCProfiler.class)
                .addProfiler(PausesProfiler.class)
                .addProfiler(CompilerProfiler.class)
                .addProfiler(StackProfiler.class)
                .addProfiler(ClassloaderProfiler.class)
                .addProfiler(HotspotMemoryProfiler.class)
                .addProfiler(HotspotClassloadingProfiler.class)
                .addProfiler(HotspotThreadProfiler.class)
                .addProfiler(HotspotRuntimeProfiler.class)
                .addProfiler(HotspotCompilationProfiler.class)
                .build();
        new Runner(opt).run();
    }

}
