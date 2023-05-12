package com.fortify.cli.common.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString @EqualsAndHashCode
public class Counter {
    @Getter private long count = 0;
    
    public Counter increase() {
        count++;
        return this;
    }
    
    public Counter increase(long value) {
        count+=value;
        return this;
    }
    
    public Counter increase(Counter counter) {
        return increase(counter.getCount());
    }
}
