package org.prefix_manager;

public class Prefix {
    private final String id;
    private final String displayName;
    private final String symbol;
    private final String color;
    private final String description;
    private final UnlockType unlockType;
    private final String unlockCondition;
    private final boolean hidden;

    public enum UnlockType {
        DEFAULT,
        ACHIEVEMENT,
        ADMIN,
        SPECIAL,
        SECRET
    }

    public Prefix(String id, String displayName, String symbol, String color, String description, UnlockType unlockType, String unlockCondition, boolean hidden) {
        this.id = id;
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
        this.description = description;
        this.unlockType = unlockType;
        this.unlockCondition = unlockCondition;
        this.hidden = hidden;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getFormattedSymbol() {
        return color + symbol + "§r";
    }

    public String getColor() { return color;}

    public String getDescription() {
        return description;
    }

    public UnlockType getUnlockType() {
        return unlockType;
    }

    public String getUnlockCondition() {
        return unlockCondition;
    }

    public boolean isHidden() {
        return hidden;
    }
} 