package com.kj.repo.test;

import org.openjdk.jol.info.ClassLayout;

public class JOI {

    public static void main(String[] args) {
        System.out.println(ClassLayout.parseInstance(new Object()).toPrintable());
    }

}
