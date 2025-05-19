package com.fortify.cli.aviator.fpr.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrimaryTag {
    private String primaryTagGUID;
    private int neutralWeight;
    private String openRange;
    private String naiRange;
}