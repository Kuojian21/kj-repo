package com.repo.test.netty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Definition for singly-linked list. public class ListNode { int val; ListNode
 * next; ListNode(int x) { val = x; } }
 */
public class Solution {
    public static void main(String[] args) {
        System.out.println(Solution.class.getSimpleName());
        System.out.println(
                new Solution().divide("wordgoodgoodgoodbestword", new String[] {"word", "good", "best", "good"}));
        ThreadLocal<Integer> threadLocal = new ThreadLocal<Integer>();
        threadLocal.set(1);
        while (true) {
            System.gc();
            System.out.println(threadLocal.get());
        }
    }

    public List<Integer> divide(String s, String[] words) {

        List<Integer> rtn = new ArrayList<Integer>();
        int len = s.length();
        int wLen = words[0].length();
        for (int i = 0; i < len; i++) {
            List<String> list = new ArrayList<String>();
            for (String word : words) {
                list.add(word);
            }
            for (int j = i + wLen; j <= len; j += wLen) {
                String t = s.substring(j - wLen, j);
                if (list.contains(t)) {
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        if (iterator.next().equals(t)) {
                            iterator.remove();
                            break;
                        }
                    }
                    if (list.size() == 0) {
                        rtn.add(i);
                    }
                } else {
                    break;
                }
            }
        }
        return rtn;
    }
}