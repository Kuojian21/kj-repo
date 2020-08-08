package com.kj.repo.test.leetcode;

import java.util.HashSet;
import java.util.Set;

class Solution {
    public boolean canFinish(int numCourses, int[][] prerequisites) {
        Set<Integer> set1 = new HashSet<Integer>();
        Set<Integer> set2 = new HashSet<Integer>();
        for (int i = 0; i < prerequisites.length; i++) {
            set1.add(prerequisites[i][0]);
            for (int j = 1; j < prerequisites[i].length; j++) {
                set2.add(prerequisites[i][j]);
            }
        }
        set2.removeAll(set1);
        numCourses = numCourses - set2.size();
        while (numCourses > 0) {
            boolean f = false;
            for (int i = 0; i < prerequisites.length; i++) {
                int j = 1;
                for (; j < prerequisites[i].length; j++) {
                    if (!set2.contains(prerequisites[i][j])) {
                        break;
                    }
                }
                if (j == prerequisites[i].length) {
                    f = true;
                    set2.add(prerequisites[i][0]);
                    set1.remove(prerequisites[i][0]);
                    numCourses--;
                }
            }
            if (!f) {
                return false;
            }
        }
        return true;
    }
}