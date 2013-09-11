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
        try (Allocheck _ = new Allocheck(1, 1, Thread.currentThread())) {
            for (int i = 0; i < 5; i++) {
                doSomeMoreWork();
            }
        }
    }

    private void doSomeMoreWork() {
        System.out.println("Doing more work");
    }

    public static void main(String[] args) throws Exception {
        new Main().run();
    }
}
