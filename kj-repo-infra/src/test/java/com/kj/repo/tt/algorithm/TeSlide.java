package com.kj.repo.tt.algorithm;

import com.kj.repo.infra.algorithm.Slide;

public class TeSlide {

    public static void main(String[] args) {
        rate();
    }

    public static void rate() {
        Slide slide = new Slide(10000);
        for (int i = 0; i < 10; i++) {
            int x = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        if (slide.tryAcquire()) {
                            System.out.println("Hi,World! " + x);
                        } else {
                            System.out.println("限流" + x);
                        }
                    }
                }
            }).start();
        }
        while (true) {
            if (slide.tryAcquire()) {
                System.out.println("Hi,World!");
            } else {
                System.out.println("限流");
            }
        }
    }

}
