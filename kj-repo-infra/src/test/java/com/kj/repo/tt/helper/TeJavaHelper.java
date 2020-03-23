package com.kj.repo.tt.helper;

import com.kj.repo.infra.helper.JavaHelper;

/**
 * @author kj
 * Created on 2020-03-23
 */
public class TeJavaHelper {
    public static void main(String[] args) {
        run();
    }

    public static void run() {
        JavaHelper.stack(1).getClassName();
        System.out.println();
        JavaHelper.stack(2).getClassName();
        System.out.println();
        JavaHelper.stack(3).getClassName();
    }
}
