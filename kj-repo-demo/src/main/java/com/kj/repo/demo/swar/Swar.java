package com.kj.repo.demo.swar;

/**
 * @author kj
 * Created on 2020-07-31
 */
public class Swar {
    public static void main(String[] args) {
        for (int i = 0; i < 128; i++) {
            System.out.println(i + ":" + swar(i));
        }
        System.out.println(swar(1025));
    }

    public static long swar(long l) {
        l = (l & 0x5555555555555555L) + ((l >> 1) & 0x5555555555555555L);
        l = (l & 0x3333333333333333L) + ((l >> 2) & 0x3333333333333333L);
        l = (l & 0x0f0f0f0f0f0f0f0fL) + ((l >> 4) & 0x0f0f0f0f0f0f0f0fL);
        l = (l & 0x00ff00ff00ff00ffL) + ((l >> 8) & 0x00ff00ff00ff00ffL);
        l = (l & 0x0000ffff0000ffffL) + ((l >> 16) & 0x0000ffff0000ffffL);
        l = (l & 0x00000000ffffffffL) + ((l >> 32) & 0x00000000ffffffffL);
        return l;
    }
}
