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

    private final boolean debug = true;

    private boolean active = false;
    private final List<Allocation> allocations;
    private final int maxObjects;
    private final long maxBytes;
    private final Thread thread;
    private int allocationCount;
    private long allocationSize;


    final Sampler sampler = new Sampler() {
        @Override
        public void sampleAllocation(int count, String desc, Object newObj, long size) {
            if (active && Thread.currentThread() == thread && newObj.getClass() != Allocheck.class) {
                allocationCount++;
                allocationSize += size;
                if (debug) {
                    active = false;
                    try {
                        String stack = Throwables.getStackTraceAsString(new Throwable());
                        allocations.add(new Allocation(desc, stack, size, System.currentTimeMillis()));
                    } finally {
                        active = true;
                    }
                }
            }
        }
    };

    // TODO friendlier api
    Allocheck(int maxObjects, long maxBytes) {
        this.thread = Thread.currentThread();
        this.maxObjects = maxObjects;
        this.maxBytes = maxBytes;
        if (debug) {
            allocations = Lists.newArrayList();
        } else {
            allocations = null;
        }
        AllocationRecorder.addSampler(sampler);
        active = true;
    }

    @Override
    public void close() {
        active = false;
        AllocationRecorder.removeSampler(sampler);

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
