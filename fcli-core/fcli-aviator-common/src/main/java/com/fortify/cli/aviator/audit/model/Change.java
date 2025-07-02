package com.fortify.cli.aviator.audit.model;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Reflectable
public class Change {
    private String file;
    private String fromLine; // Corresponds to from_line in proto
    private String toLine;   // Corresponds to to_line in proto
    private String replaceWith;
}