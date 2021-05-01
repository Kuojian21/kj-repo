package guava.graph;

import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Solution solution = new Solution();
        System.out.println(solution.canCross(new int[]{0,1,3,5,6,8,12,17}));
    }

    public boolean canCross(int[] stones) {
        int n = stones.length;
        boolean[][] dp = new boolean[n][n];

        dp[0][0] = true;
        for(int i = 1; i < n; i++){
            for(int j = i - 1; j >= 0;j--){
                int k = stones[i] - stones[j];
                if(k > j + 1){
                    continue;
                }
                dp[i][k] = dp[j][k - 1] || dp[j][k] || dp[j][k+1];
                if(i == n - 1){
                    if(dp[i][k]){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean dfs(int[] stones,int i,int k){
        if(i >= stones.length - 1){
            return true;
        }else if((stones[i] + k - 1 > stones[i+1]) || (stones[i] + k + 1 < stones[i+1])){
            return false;
        }else{
            return dfs(stones,i+1,stones[i+1] - stones[i]);
        }
    }
}