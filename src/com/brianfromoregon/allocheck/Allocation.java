package com.brianfromoregon.allocheck;

/**
 *
 */
class Allocation {
    final String type;
    final String stack;
    final long bytes;
    final long time;

    public Allocation(String type, String stack, long bytes, long time) {
        this.type = type;
        this.stack = stack;
        this.bytes = bytes;
        this.time = time;
    }
}
