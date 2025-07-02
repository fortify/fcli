package com.fortify.cli.aviator.audit.model;

import java.util.List;
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
public class Autoremediation {
    private List<Change> changes;
}