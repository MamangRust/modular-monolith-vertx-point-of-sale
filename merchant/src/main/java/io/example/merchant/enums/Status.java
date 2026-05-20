package io.example.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
    PENDING("pending"),
    ACTIVE("active"),
    INACTIVE("inactive"),
    SUCCESS("success"),
    FAILED("failed"),
    REJECTED("rejected");

    private final String value;

    public static Status fromString(String str) {
        if (str == null) {
            return PENDING;
        }
        for (Status s : Status.values()) {
            if (s.name().equalsIgnoreCase(str) || s.getValue().equalsIgnoreCase(str)) {
                return s;
            }
        }
        return PENDING;
    }
}
