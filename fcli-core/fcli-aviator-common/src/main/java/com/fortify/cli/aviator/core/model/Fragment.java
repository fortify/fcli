package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
