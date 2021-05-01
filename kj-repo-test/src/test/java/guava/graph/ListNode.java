package guava.graph;

 public class ListNode {
     int val;
     ListNode next;
     ListNode() {}
     ListNode(int val) { this.val = val; }
     ListNode(int val, ListNode next) { this.val = val; this.next = next; }

     public static ListNode gen(int[] nums){
         ListNode head = new ListNode(nums[0]);
         ListNode p = head;
         for(int i = 1; i < nums.length;i++){
             p.next = new ListNode(nums[i]);
             p = p.next;
         }
         return head;
     }
 }
