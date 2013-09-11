You can introduce a try-with-resources block to specify the maximum allowed # of objects allocated and/or # bytes
allocated by the inside of that try block.

For example:

    void doSomeWork() {
        try (Allocheck _ = new Allocheck(1, 1)) {
            for (int i = 0; i < 5; i++) {
                new Object().hashCode();
                Integer.parseInt(String.valueOf(i));
            }
        }
    }

Will output something like this:

    [Number of objects allocated exceeded max: 15 > 1, Number of bytes allocated exceeded max: 240 > 1]

    Allocations by type, ordered by count:
    "Count","Total Bytes","Type"
    "5","40","java/lang/Object"
    "5","80","char"
    "5","120","java/lang/String"

    Allocations by code path, ordered by total bytes:
    "Count","Total Bytes","Type and Stack"
    "5","120","java/lang/String
        at java.lang.Integer.toString(Integer.java:333)
        at java.lang.String.valueOf(String.java:2959)
        at com.brianfromoregon.allocheck.Main.doSomeWork(Main.java:19)"
    "5","80","char
        at java.lang.Integer.toString(Integer.java:331)
        at java.lang.String.valueOf(String.java:2959)
        at com.brianfromoregon.allocheck.Main.doSomeWork(Main.java:19)"
    "5","40","java/lang/Object
        at com.brianfromoregon.allocheck.Main.doSomeWork(Main.java:18)"