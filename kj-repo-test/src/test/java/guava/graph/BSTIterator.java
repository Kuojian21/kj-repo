package guava.graph;

import java.util.Stack;

class BSTIterator {

    public static void main(String[] args) {
        BSTIterator iterator = new BSTIterator(TreeNode.gen(new Integer[]{7, 3, 15, null, null, 9, 20}));
        System.out.println(iterator.next());
        System.out.println(iterator.next());        System.out.println(iterator.next());

        iterator.next();
        iterator.next();
    }

    private Stack<TreeNode> stack = new Stack<TreeNode>();
    public BSTIterator(TreeNode node) {
        this.push(node);
    }
    
    public int next() {
        if(!hasNext()){
            return -1;
        }

        TreeNode node = this.stack.pop();
        if(node.right != null){
            this.push(node.right);
        }
        return node.val;
    }

    private void push(TreeNode node){
        this.stack.add(node);
        while(node.left != null){
            this.stack.push(node.left);
            node = node.left;
        }
    }

    public boolean hasNext() {
        return !this.stack.isEmpty();
    }
}