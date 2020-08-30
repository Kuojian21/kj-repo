package com.kj.repo.test.leetcode;

class Solution {
    public void solveSudoku(char[][] board) {
        int[][] row = new int[9][9];
        int[][] col = new int[9][9];
        int[][] rec = new int[9][9];
        solve(board, 0, row, col, rec);
    }

    public boolean solve(char[][] board, int i, int[][] row, int[][] col, int[][] rec) {
        if (i >= 81) {
            return true;
        }
        int r = i / 9;
        int c = i % 9;
        if (board[r][c] != '.') {
            if (isValid(board, row, col, rec, r, c)) {
                row[r][board[r][c] - '1']++;
                col[c][board[r][c] - '1']++;
                rec[r / 3 * 3 + c / 3][board[r][c] - '1']++;
                boolean rtn = solve(board, i + 1, row, col, rec);
                if (rtn) {
                    return true;
                }
                row[r][board[r][c] - '1']--;
                col[c][board[r][c] - '1']--;
                rec[r / 3 * 3 + c / 3][board[r][c] - '1']--;
                return false;
            }
            return false;
        } else {
            for (char cc : new char[] {'1', '2', '3', '4', '5', '6', '7', '8', '9'}) {
                board[r][c] = cc;
                if (isValid(board, row, col, rec, r, c)) {
                    row[r][board[r][c] - '1']++;
                    col[c][board[r][c] - '1']++;
                    rec[r / 3 * 3 + c / 3][board[r][c] - '1']++;
                    boolean rtn = solve(board, i + 1, row, col, rec);
                    if (rtn) {
                        return true;
                    }
                    row[r][board[r][c] - '1']--;
                    col[c][board[r][c] - '1']--;
                    rec[r / 3 * 3 + c / 3][board[r][c] - '1']--;
                }
            }
            return false;
        }
    }

    public boolean isValid(char[][] board, int[][] row, int[][] col, int[][] rec, int r, int c) {
        if (row[r][board[r][c] - '1'] >= 1) {
            return false;
        }
        if (col[c][board[r][c] - '1'] >= 1) {
            return false;
        }
        if (rec[r / 3 * 3 + c / 3][board[r][c] - '1'] >= 1) {
            return false;
        }
        return true;
    }
}