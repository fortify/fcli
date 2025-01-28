package com.fortify.cli.aviator.core.model;

public enum Tier {
    GOLD("GOLD"),
    SILVER("SILVER"),
    UNSUPPORTED("UNSUPPORTED");


    private final String value;

    Tier(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Tier fromString(String value) {
        if (value == null) {
            return null;
        }

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            for (Tier tier : values()) {
                if (tier.value.equalsIgnoreCase(value)) {
                    return tier;
                }
            }
            return null;
        }
    }

    public boolean matches(Object other) {
        if (other == null) {
            return false;
        }

        if (other instanceof Tier) {
            return this == other;
        }

        if (other instanceof String) {
            return value.equalsIgnoreCase((String) other);
        }

        return false;
    }

}
