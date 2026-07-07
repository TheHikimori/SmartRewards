package me.azuredev.smartrewards.rewards;

public final class ClaimResult {

    public enum Type {
        SUCCESS,
        ALREADY_CLAIMED,
        NOT_AVAILABLE,
        DISABLED,
        ERROR
    }

    public static final ClaimResult SUCCESS = new ClaimResult(Type.SUCCESS, null);
    public static final ClaimResult ALREADY_CLAIMED = new ClaimResult(Type.ALREADY_CLAIMED, null);
    public static final ClaimResult NOT_AVAILABLE = new ClaimResult(Type.NOT_AVAILABLE, null);
    public static final ClaimResult DISABLED = new ClaimResult(Type.DISABLED, null);
    public static final ClaimResult ERROR = new ClaimResult(Type.ERROR, null);

    private final Type type;
    private final String message;

    private ClaimResult(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public static ClaimResult notAvailable(String message) {
        return new ClaimResult(Type.NOT_AVAILABLE, message);
    }

    public static ClaimResult error(String message) {
        return new ClaimResult(Type.ERROR, message);
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }
}
