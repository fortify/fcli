package com.fortify.cli.aviator.fpr.filter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class TagDefinition {
    private String id;
    private String type;
    private boolean extensible;
    private boolean hidden;
    private boolean isIconTag;
    private String restriction;
    private int objectVersion;
    private String valueType;
    private String name;
    private String description;
    private List<TagValue> values;
    private List<String> valuesStr;

    public TagDefinition(String name, String id, List<String> values, boolean extensible){
        this.name = name;
        this.id = id;
        this.valuesStr = values;
        this.extensible = extensible;
    }

    public List<String> getTagValuesAsString() {
        return values.stream()
                .map(TagValue::getValue)
                .collect(Collectors.toList());
    }
}