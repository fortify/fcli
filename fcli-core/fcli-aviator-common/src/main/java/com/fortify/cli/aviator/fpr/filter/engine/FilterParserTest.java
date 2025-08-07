package com.fortify.cli.aviator.fpr.filter.engine;


import com.fortify.cli.aviator.fpr.filter.SearchTree;

public class FilterParserTest {
    private static int testCount = 0;
    private static int passCount = 0;

    public static void main(String[] args) {
        System.out.println("--- RUNNING FILTER PARSER TESTS ---");

        test("[fortify priority order]:critical", 1);
        test("impact:![2.5, 5.0]", 1);
        test("likelihood:![1.0, 5.0]", 1);
        test("category:Insecure Dependency: Vulnerable Component [analysis type]:SCA", 3);
        test("[PCI 4.0]:<none> AND [fortify priority order]:low", 3);
        test("[AA_Prediction]:\"Indeterminate (Below Not An Issue threshold)\" or [AA_Prediction]:\"Indeterminate (Below Exploitable threshold)\"", 3);
        test("audience:!fod analyzer:!pentest OR category:\"SQL Injection\"", 5);

        System.out.println("\n--- TEST SUMMARY ---");
        System.out.printf("PASSED: %d/%d%n", passCount, testCount);
        if (passCount != testCount) {
            System.out.println("!!! SOME TESTS FAILED !!!");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY.");
        }
    }

    private static int countTreeNodes(SearchTree.Node node) {
        if (node == null) return 0;
        if (node.isLeaf()) return 1;
        return 1 + countTreeNodes(node.getLeftChild()) + countTreeNodes(node.getRightChild());
    }

    private static void test(String query, int expectedNodes) {
        testCount++;
        System.out.println("\n[TEST " + testCount + "] Query: " + query);
        try {
            SearchTree tree = FilterParser.parse(query);
            int nodeCount = countTreeNodes(tree.getRoot());

            System.out.println("  Tree Structure: " + (tree.getRoot() != null ? tree.getRoot().toString() : "null"));

            if (nodeCount == expectedNodes) {
                System.out.println("  Result: PASSED (Node count: " + nodeCount + ")");
                passCount++;
            } else {
                System.out.println("  Result: FAILED (Expected " + expectedNodes + " nodes, but got " + nodeCount + ")");
            }
        } catch (Exception e) {
            System.out.println("  Result: FAILED (Threw Exception)");
            e.printStackTrace(System.out);
        }
    }
}