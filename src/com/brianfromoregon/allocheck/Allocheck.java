package com.brianfromoregon.allocheck;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.Sampler;

import java.util.List;

import static java.lang.String.format;

/**
 *
 */
public class Allocheck implements AutoCloseable {

    private static boolean globalEnabled = true;
    private static boolean globalDebug = true;

    private final boolean debug = globalDebug;

    private boolean enabled = globalEnabled;
    private final List<Allocation> allocations;
    private final int maxObjects;
    private final long maxBytes;
    private final Thread thread;
    private int allocationCount;
    private long allocationSize;


    final Sampler sampler = new Sampler() {
        @Override
        public void sampleAllocation(int count, String desc, Object newObj, long size) {
            if (enabled && Thread.currentThread() == thread) {
                allocationCount++;
                allocationSize += size;
                if (debug) {
                    enabled = false;
                    try {
                        String stack = Throwables.getStackTraceAsString(new Throwable());
                        allocations.add(new Allocation(desc, stack, size, System.currentTimeMillis()));
                    } finally {
                        enabled = true;
                    }
                }
            }
        }
    };

    // TODO friendlier api
    Allocheck(int maxObjects, long maxBytes, Thread thread) {
        this.thread = thread;
        this.maxObjects = maxObjects;
        this.maxBytes = maxBytes;
        if (enabled) {
            if (debug) {
                allocations = Lists.newArrayList();
            } else {
                allocations = null;
            }
            AllocationRecorder.addSampler(sampler);
        } else {
            allocations = null;
        }
    }

    @Override
    public void close() {
        if (!enabled) {
            return;
        }

        AllocationRecorder.removeSampler(sampler);

        if (enabled) {
            List<String> violations = Lists.newArrayList();
            if (allocationCount > maxObjects) {
                violations.add(format("Number of objects allocated exceeded max: %d > %d", allocationCount, maxObjects));
            }

            if (allocationSize > maxBytes) {
                violations.add(format("Number of bytes allocated exceeded max: %d > %d", allocationSize, maxBytes));

            }

            if (!violations.isEmpty()) {
                throw new AllocationException(violations, allocations);
            }
        }
    }
}
