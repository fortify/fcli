package com.fortify.cli.aviator.fpr.filter;


import lombok.Getter;

/**
 * Represents the entire logical filter query as a binary tree.
 */
public class SearchTree {
    public enum LogicalOperator { AND, OR }

    @Getter
    private final Node root;

    public SearchTree(Node root) {
        this.root = root;
    }

    public String toString() {
        return root == null ? "<null>" : root.toString();
    }

    /**
     * Represents a node in the search tree. Can be either a leaf (query) or an operator.
     */
    public static class Node {
        private final LogicalOperator operator;
        private final SearchQuery query;
        private Node leftChild;
        private Node rightChild;

        // Constructor for a leaf node
        public Node(SearchQuery query) {
            this.query = query;
            this.operator = null;
        }

        // Constructor for an operator node
        public Node(LogicalOperator operator, Node left, Node right) {
            this.operator = operator;
            this.leftChild = left;
            this.rightChild = right;
            this.query = null;
        }

        public String toString() {
            if (isLeaf()) return query.toString();
            return "(" + leftChild.toString() + " " + operator + " " + rightChild.toString() + ")";
        }

        public boolean isLeaf() {
            return operator == null;
        }

        // --- ADD THESE GETTERS ---
        public LogicalOperator getOperator() {
            return operator;
        }

        public SearchQuery getQuery() {
            return query;
        }

        public Node getLeftChild() {
            return leftChild;
        }

        public Node getRightChild() {
            return rightChild;
        }
    }
}