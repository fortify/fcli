package com.fortify.cli.aviator.fpr.filter.engine;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.Filter;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.VulnerabilityFilterer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpactFilterTest {

    public static void main(String[] args) {
        System.out.println("--- RUNNING REFINED IMPACT HIDE FILTER TEST ---");

        // 1. Setup the FilterSet and the specific "hide" filter
        FilterSet quickViewFilterSet = new FilterSet();
        quickViewFilterSet.setTitle("Quick View Test");

        Filter hideImpactFilter = new Filter();
        hideImpactFilter.setAction("hide");
        hideImpactFilter.setQuery("impact:![2.5, 5.0]");

        Filter hideLikelihoodFilter = new Filter();
        hideLikelihoodFilter.setAction("hide");
        hideLikelihoodFilter.setQuery("likelihood:![1.0, 5.0]");

        // A FilterSet needs "setFolder" rules to define the universe of issues.
        Filter folderFilter = new Filter();
        folderFilter.setAction("setFolder");
        // This query makes all issues candidates for filtering.
        folderFilter.setQuery("[fortify priority order]:critical OR [fortify priority order]:high OR [fortify priority order]:medium OR [fortify priority order]:low");

        List<Filter> filters = new ArrayList<>();
        filters.add(folderFilter);
        filters.add(hideImpactFilter);
        filters.add(hideLikelihoodFilter);
        quickViewFilterSet.setFilters(filters);

        // 2. Create test vulnerabilities using the default constructor and public setters

        // Case 1: Should be HIDDEN (low impact)
        Vulnerability vulnToHide = new Vulnerability();
        vulnToHide.setInstanceID("SHOULD_BE_HIDDEN_IMPACT"); // This now works
        vulnToHide.setImpact(2.0);
        vulnToHide.setLikelihood("3.0"); // Likelihood is OK
        vulnToHide.setPriority("Low");

        // Case 2: Should be KEPT (impact and likelihood are within the "good" range)
        Vulnerability vulnToKeep = new Vulnerability();
        vulnToKeep.setInstanceID("SHOULD_BE_KEPT"); // This now works
        vulnToKeep.setImpact(3.0);
        vulnToKeep.setLikelihood("3.0");
        vulnToKeep.setPriority("Medium");

        // Case 3: Should be HIDDEN (low likelihood)
        Vulnerability vulnToHide2 = new Vulnerability();
        vulnToHide2.setInstanceID("SHOULD_BE_HIDDEN_LIKELIHOOD"); // This now works
        vulnToHide2.setImpact(3.0); // Impact is OK
        vulnToHide2.setLikelihood("0.5"); // Likelihood is too low
        vulnToHide2.setPriority("Low");

        List<Vulnerability> allVulnerabilities = new ArrayList<>();
        allVulnerabilities.add(vulnToHide);
        allVulnerabilities.add(vulnToKeep);
        allVulnerabilities.add(vulnToHide2);

        System.out.println("Initial vulnerabilities: " + allVulnerabilities.size());

        // 3. Run the filtering logic directly
        List<Vulnerability> filteredResult = filterVulnerabilities(allVulnerabilities, quickViewFilterSet);

        System.out.println("\nApplying filters...");
        System.out.println("Final vulnerabilities after filtering: " + filteredResult.size());
        for (Vulnerability v : filteredResult) {
            System.out.println(" - " + v.getInstanceID() + " (Impact: " + v.getImpact() + ", Likelihood: " + v.getLikelihood() + ")");
        }

        // 4. Assert the result using the clear instance ID
        boolean testPassed = filteredResult.size() == 1 &&
                filteredResult.get(0).getInstanceID().equals("SHOULD_BE_KEPT");

        System.out.println("\n--- TEST SUMMARY ---");
        if (testPassed) {
            System.out.println("PASSED: The filter logic correctly isolated the expected vulnerability.");
        } else {
            System.out.println("FAILED: The filter did not produce the expected result.");
            System.exit(1);
        }
    }

    /**
     * This is the standalone, correct implementation of the filter logic,
     * copied from IssueAuditor to make this test self-contained.
     */
    public static List<Vulnerability> filterVulnerabilities(List<Vulnerability> vulnerabilities, FilterSet fs) {
        if (fs == null || fs.getFilters() == null || fs.getFilters().isEmpty() || vulnerabilities == null) {
            return vulnerabilities;
        }

        List<Filter> folderFilters = fs.getFilters().stream()
                .filter(f -> "setFolder".equalsIgnoreCase(f.getAction()))
                .collect(Collectors.toList());

        List<Filter> hideFilters = fs.getFilters().stream()
                .filter(f -> "hide".equalsIgnoreCase(f.getAction()))
                .collect(Collectors.toList());

        Set<Vulnerability> candidateVulnerabilities = new HashSet<>();

        if (folderFilters.isEmpty()) {
            candidateVulnerabilities.addAll(vulnerabilities);
        } else {
            for (Filter folderFilter : folderFilters) {
                List<Vulnerability> matched = VulnerabilityFilterer.filter(vulnerabilities, folderFilter.getQuery());
                candidateVulnerabilities.addAll(matched);
            }
        }

        Set<Vulnerability> hiddenVulnerabilities = new HashSet<>();
        for (Filter hideFilter : hideFilters) {
            List<Vulnerability> matchedToHide = VulnerabilityFilterer.filter(new ArrayList<>(candidateVulnerabilities), hideFilter.getQuery());
            hiddenVulnerabilities.addAll(matchedToHide);
        }

        candidateVulnerabilities.removeAll(hiddenVulnerabilities);

        return new ArrayList<>(candidateVulnerabilities);
    }
}