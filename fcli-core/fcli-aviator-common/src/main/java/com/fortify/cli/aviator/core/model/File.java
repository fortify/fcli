package com.fortify.cli.aviator.core.model;

import com.formkiq.graalvm.annotations.Reflectable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Reflectable
@NoArgsConstructor
@Setter
public class File {
    private String name;
    private String content;
    private boolean segment;
    private int startLine;
    private int endLine;
}
