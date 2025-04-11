package iuh.fit.se.model;

public enum ConversationType {
    PRIVATE("private"),
    GROUP("group");

    private final String value;

    ConversationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConversationType fromValue(String value) {
        for (ConversationType type : ConversationType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
