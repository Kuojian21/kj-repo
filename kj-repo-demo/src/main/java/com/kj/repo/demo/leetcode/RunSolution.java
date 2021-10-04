package com.kj.repo.demo.leetcode;

/**
 * @author kj
 * Created on 2020-08-02
 */
public class RunSolution {
    public static void main(String[] args) {
        Solution solution = new Solution();
        //        pri(solution.reverseKGroup(gen(new int{} {1, 2, 3, 4, 5}), 2));
        //        pri(gen(new int{} {1, 2, 3, 4, 5}));

        solution.solveSudoku(new char[][] {{'5', '3', '.', '.', '7', '.', '.', '.', '.'},
                {'6', '.', '.', '1', '9', '5', '.', '.', '.'}, {'.', '9', '8', '.', '.', '.', '.', '6', '.'},
                {'8', '.', '.', '.', '6', '.', '.', '.', '3'}, {'4', '.', '.', '8', '.', '3', '.', '.', '1'},
                {'7', '.', '.', '.', '2', '.', '.', '.', '6'}, {'.', '6', '.', '.', '.', '.', '2', '8', '.'},
                {'.', '.', '.', '4', '1', '9', '.', '.', '5'}, {'.', '.', '.', '.', '8', '.', '.', '7', '9'}});
    }

    public static ListNode gen(int[] vals) {
        ListNode dumpy = new ListNode(0);
        ListNode x = dumpy;
        for (int val : vals) {
            x.next = new ListNode(val);
            x = x.next;
        }
        return dumpy.next;
    }

    public static void pri(ListNode node) {
        System.out.print(node.val);
        while (node.next != null) {
            System.out.print(',' + node.next.val);
            node = node.next;
        }
        System.out.println();
    }
}
