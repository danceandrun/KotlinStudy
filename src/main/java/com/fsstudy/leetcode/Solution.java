package com.fsstudy.leetcode;

import java.util.PriorityQueue;

public class Solution {

    public static class ListNode {
        int val;
        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    /**
     * 23.合并K个升序列表
     * 使用了优先级队列
     */
    public ListNode mergeKLists(ListNode[] lists) {
        if (lists.length == 0) return null;
        ListNode dummy = new ListNode(-1);
        ListNode p = dummy;
        PriorityQueue<ListNode> pq = new PriorityQueue<>( lists.length, (a, b) -> (a.val - b.val));
        for (ListNode head : lists) {
            if (head != null) pq.add(head);
        }

        while (!pq.isEmpty()) {
            ListNode node = pq.poll();
            p.next = node;
            if (node.next != null) pq.add(node.next);
            p = p.next;
        }
        return dummy.next;
    }
}
