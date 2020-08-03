package com.kj.repo.test.leetcode;

import java.util.Stack;

class Solution {
    public boolean isValid(String s) {
        if (s == null) {
            return false;
        }
        Stack stack = new Stack();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else if (c == ')') {
                char tc = (Character) stack.pop();
                if (tc != '(') {
                    return false;
                }
            } else if (c == ']') {
                char tc = (Character) stack.pop();
                if (tc != '[') {
                    return false;
                }
            } else if (c == '}') {
                char tc = (Character) stack.pop();
                if (tc != '{') {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }
}