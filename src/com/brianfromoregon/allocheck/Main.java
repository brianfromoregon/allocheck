package com.brianfromoregon.allocheck;

/**
 *
 */
public class Main {


    public void run() {
        for (int i = 0; i < 5; i++) {
            doSomeWork();
        }
    }

    void doSomeWork() {
        try (Allocheck _ = new Allocheck(1, 1)) {
            for (int i = 0; i < 5; i++) {
                new Object().hashCode();
                Integer.parseInt(String.valueOf(i));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run();
    }
}
