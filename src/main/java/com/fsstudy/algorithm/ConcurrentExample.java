package com.fsstudy.algorithm;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentExample {
    private static final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                map.put("key" + i, i);
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                map.put("key" + i, i * 10);
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("Map size: " + map.size());
    }

}
