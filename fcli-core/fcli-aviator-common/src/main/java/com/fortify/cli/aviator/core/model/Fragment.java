package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Reflectable
public class Fragment {
    private String content;
    private int startLine;
    private int endLine;
}
