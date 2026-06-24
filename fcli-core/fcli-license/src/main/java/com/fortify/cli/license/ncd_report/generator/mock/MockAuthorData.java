/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.ncd_report.generator.mock;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Mock author dataset with realistic names and built-in duplicate scenarios
 * for testing deduplication and AI detection functionality.
 */
@Data @AllArgsConstructor
public class MockAuthorData {
    private String name;
    private String email;
    
    /** Predefined realistic authors with duplicates and AI candidates */
    private static final List<MockAuthorData> REALISTIC_AUTHORS = Arrays.asList(
        // Real distinct authors
        new MockAuthorData("John Smith", "john.smith@example.com"),
        new MockAuthorData("Sarah Johnson", "sarah.j@example.com"),
        new MockAuthorData("Michael Chen", "m.chen@example.com"),
        new MockAuthorData("Emma Williams", "emma.williams@example.com"),
        new MockAuthorData("Alex Rodriguez", "alex.r@example.com"),
        
        // DUPLICATE SCENARIOS: Same person, different name variations
        // John Smith via different name formats
        new MockAuthorData("J. Smith", "john.smith@example.com"),
        new MockAuthorData("John D. Smith", "john.smith@example.com"),
        
        // Sarah Johnson via nickname
        new MockAuthorData("Sara Johnson", "sarah.j@example.com"),
        new MockAuthorData("Sarah J.", "sarah.j@example.com"),
        
        // AI CANDIDATE DUPLICATES: Very similar names, different emails
        // Could be same person with different company emails
        new MockAuthorData("Michael Chen", "mchen@company.com"),
        new MockAuthorData("Mike Chen", "mike.chen@example.com"),
        
        // Very similar variations (typos or transliteration)
        new MockAuthorData("Emma Willams", "emma.w@example.com"),  // typo
        new MockAuthorData("Emma Williams", "ewilliams@example.com"),  // same name, different email
        
        // Common name variations
        new MockAuthorData("Alex Rodriguez", "arodriguez@example.com"),
        new MockAuthorData("Alexander Rodriguez", "alex.r@example.com"),
        
        // Additional distinct authors for volume
        new MockAuthorData("Jennifer Park", "j.park@example.com"),
        new MockAuthorData("David Brown", "d.brown@example.com"),
        new MockAuthorData("Lisa Anderson", "l.anderson@example.com"),

        // Some ignored authors (e.g., bots, test accounts)
        new MockAuthorData("MyBot1 [bot]", "bot1@example.com"),
        new MockAuthorData("MyBot2 [bot]", "bot2@example.com"),
        new MockAuthorData("MyBot3 [bot]", "bot3@example.com"),
        new MockAuthorData("MyBot4 [bot]", "bot4@example.com")
    );
    
    /**
     * Get a predefined realistic author by index, cycling through the list.
     */
    public static MockAuthorData getRealisticAuthor(int index) {
        return REALISTIC_AUTHORS.get(index % REALISTIC_AUTHORS.size());
    }
    
    /**
     * Get total number of realistic authors defined.
     */
    public static int getRealisticAuthorCount() {
        return REALISTIC_AUTHORS.size();
    }
    
    /**
     * Get all realistic authors.
     */
    public static List<MockAuthorData> getAllRealisticAuthors() {
        return REALISTIC_AUTHORS;
    }
}
