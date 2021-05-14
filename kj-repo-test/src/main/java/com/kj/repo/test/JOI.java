package com.kj.repo.test;

import com.google.common.util.concurrent.Uninterruptibles;
import org.openjdk.jol.info.ClassLayout;

import java.util.concurrent.TimeUnit;

/**
 * Java Object Layout
 */
public class JOI {
    /**
     * -XX:+UseBiasedLocking -XX:BiasedLockingStartupDelay=0
     * @param args
     */
    public static void main(String[] args) {
        final Object obj = new Object();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (obj){
//                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MINUTES);
//                }
//            }
//        }).start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                synchronized (obj){
//                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MINUTES);
//                }
//            }
//        }).start();
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
        synchronized (obj){
            System.out.println(ClassLayout.parseInstance(obj).toPrintable());
            obj.hashCode();
            System.out.println(ClassLayout.parseInstance(obj).toPrintable());
        }
        obj.hashCode();
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
        synchronized (obj) {
            System.out.println(ClassLayout.parseInstance(obj).toPrintable());
        }
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
    }

}
