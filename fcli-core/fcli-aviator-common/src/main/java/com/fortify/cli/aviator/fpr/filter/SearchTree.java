package com.fortify.cli.aviator.fpr.filter;


import lombok.Getter;

/**
 * Represents the entire logical filter query as a binary tree.
 */
@Getter
public class SearchTree {
    public enum LogicalOperator { AND, OR }

    private final Node root;

    public SearchTree(Node root) {
        this.root = root;
    }

    /**
     * Represents a node in the search tree. Can be either a leaf (query) or an operator.
     */
    @Getter
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

        public boolean isLeaf() {
            return operator == null;
        }

    }
}