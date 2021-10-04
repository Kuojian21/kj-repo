package com.kj.repo.tt.helper;

import com.kj.repo.infra.utils.JavaUtil;

/**
 * @author kj
 * Created on 2020-03-23
 */
public class TeJavaHelper {
    public static void main(String[] args) {
        run();
    }

    public static void run() {
        JavaUtil.stack(1).getClassName();
        System.out.println();
        JavaUtil.stack(2).getClassName();
        System.out.println();
        JavaUtil.stack(3).getClassName();
    }
}
