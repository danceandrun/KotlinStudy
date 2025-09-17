package com.fsstudy.leetcode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SolutionTest {
    Solution solution;

    @BeforeEach
    public void setUp(){
        solution = new Solution();
    }

    @Test
    public void testMergeKLists() {
        Solution.ListNode node1 = new Solution.ListNode(1);
        Solution.ListNode node2 = new Solution.ListNode(4);
        Solution.ListNode node3 = new Solution.ListNode(5);
        node1.next = node2;
        node2.next = node3;

        Solution.ListNode node4 = new Solution.ListNode(1);
        Solution.ListNode node5 = new Solution.ListNode(3);
        Solution.ListNode node6 = new Solution.ListNode(4);
        node4.next = node5;
        node5.next = node6;

        Solution.ListNode node7 = new Solution.ListNode(2);
        Solution.ListNode node8 = new Solution.ListNode(6);
        node7.next = node8;

        solution.mergeKLists(new Solution.ListNode[]{node1, node4, node7});
    }
}
