package com.brianfromoregon.allocheck;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.StringWriter;
import java.util.*;

import static java.lang.String.valueOf;

/**
 *
 */
public class AllocationException extends RuntimeException {
    private final List<String> violations;
    private final List<Allocation> allocations;

    public AllocationException(List<String> violations, List<Allocation> allocations) {
        this.violations = violations;
        this.allocations = allocations;
    }

    @Override
    public String toString() {
        if (allocations == null) {
            return violations.toString();
        } else {
            StringBuilder sb = new StringBuilder(violations.toString()).append("\n");
            sb.append("\nAllocations by type, ordered by count:\n");
            {
                Multimap<String, Allocation> byType = ArrayListMultimap.create();
                for (Allocation allocation : allocations) {
                    byType.put(allocation.type, allocation);
                }
                List<Row> rows = Lists.newArrayList();
                for (Map.Entry<String, Collection<Allocation>> e : byType.asMap().entrySet()) {
                    List<Allocation> list = (List<Allocation>) e.getValue();
                    long totalBytes = 0;
                    for (Allocation a : list) {
                        totalBytes += a.bytes;
                    }
                    rows.add(new Row(e.getKey(), list.size(), totalBytes));
                }
                Collections.sort(rows, new Comparator<Row>() {
                    @Override
                    public int compare(Row o1, Row o2) {
                        return Ints.compare(o2.count, o1.count);
                    }
                });
                writeRows(rows, "Type", sb);
            }

            sb.append("\nAllocations by code path, ordered by total bytes:\n");
            {
                int commonBegin = redundantPrefix(Lists.transform(allocations, new Function<Allocation, String>() {
                    @Override
                    public String apply(Allocation input) {
                        return input.stack;
                    }
                }));
                int commonTrail = redundantPrefix(Lists.transform(allocations, new Function<Allocation, String>() {
                    @Override
                    public String apply(Allocation input) {
                        return new StringBuilder(input.stack).reverse().toString();
                    }
                }));
                Multimap<String, Allocation> byStack = ArrayListMultimap.create();
                for (Allocation allocation : allocations) {
                    String stack = allocation.stack.substring(commonBegin, allocation.stack.length() - commonTrail + 1);
                    byStack.put(stack, allocation);
                }
                List<Row> rows = Lists.newArrayList();
                for (Map.Entry<String, Collection<Allocation>> e : byStack.asMap().entrySet()) {
                    List<Allocation> list = (List<Allocation>) e.getValue();
                    long totalBytes = 0;
                    for (Allocation a : list) {
                        totalBytes += a.bytes;
                    }
                    rows.add(new Row(list.get(0).type + "\n\tat " + e.getKey(), list.size(), totalBytes));
                }
                Collections.sort(rows, new Comparator<Row>() {
                    @Override
                    public int compare(Row o1, Row o2) {
                        return Longs.compare(o2.totalBytes, o1.totalBytes);
                    }
                });
                writeRows(rows, "Type and Stack", sb);
            }
            return sb.toString();
        }
    }

    private void writeRows(List<Row> rows, String stringColName, StringBuilder sb) {
        StringWriter buf = new StringWriter();
        CSVWriter csv = new CSVWriter(buf);
        csv.writeNext(new String[]{"Count", "Total Bytes", stringColName});
        for (Row row : rows) {
            csv.writeNext(new String[]{valueOf(row.count), valueOf(row.totalBytes), row.string});
        }
        sb.append(buf.toString());
    }

    private static int redundantPrefix(Iterable<String> strings) {
        List<String> sorted = Lists.newArrayList(strings);
        if (sorted.size() <= 1) {
            return 0;
        } else {
            Collections.sort(sorted);
            String first = sorted.get(0);
            String last = sorted.get(sorted.size() - 1);
            int stop = Math.min(first.length(), last.length());
            for (int i = 0; i < stop; i++) {
                if (first.charAt(i) != last.charAt(i)) {
                    return i;
                }
            }
            return stop;
        }

    }

    static class Row {
        final String string;
        final int count;
        final long totalBytes;

        Row(String string, int count, long totalBytes) {
            this.string = string;
            this.count = count;
            this.totalBytes = totalBytes;
        }
    }
}
