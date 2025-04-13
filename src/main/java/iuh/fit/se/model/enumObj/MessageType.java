package iuh.fit.se.model.enumObj;

public enum MessageType {
	TEXT("text"),
	MEDIA("media"),
	CALL("call"),
	FILE("file");

	private final String value;

	MessageType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static MessageType fromValue(String value) {
		for (MessageType type : MessageType.values()) {
			if (type.value.equals(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown value: " + value);
	}
}
