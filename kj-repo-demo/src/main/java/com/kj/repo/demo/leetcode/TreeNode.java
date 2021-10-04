package com.kj.repo.demo.leetcode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class TreeNode {
      int val;
      TreeNode left;
      TreeNode right;
      TreeNode() {}
      TreeNode(int val) { this.val = val; }
      TreeNode(int val, TreeNode left, TreeNode right) {
          this.val = val;
          this.left = left;
          this.right = right;
      }

      public static TreeNode gen(Integer[] nums){
          TreeNode root = new TreeNode(nums[0]);
          Queue<TreeNode> queue = new LinkedList<>();
          queue.add(root);
          int i = 1;
          while(i < nums.length){
              TreeNode node = queue.poll();
              if(nums[i] != null){
                  node.left = new TreeNode(nums[i]);
                  queue.add(node.left);
              }
              i++;
              if(nums[i] != null){
                  node.right = new TreeNode(nums[i]);
                  queue.add(node.right);
              }
              i++;
          }
          return root;
      }
  }