package rege.rege.silt.parser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rege.rege.silt.base.ast.NodeType;

/**
 * Container holding a parsed numeric value and its statically resolved
 * {@link NodeType}.
 *
 * @author REGE
 * @since 0.0.1a1
 */
public final class ParsedNumber {
    private final Object value;
    private final NodeType<?> type;

    /**
     * Constructs a new {@code ParsedNumber}.
     *
     * @param value parsed number value object
     * @param type node type of the number
     */
    @Contract(pure = true)
    public ParsedNumber(@NotNull Object value, @NotNull NodeType<?> type) {
        this.value = value;
        this.type = type;
    }

    /**
     * @return the boxed numeric value object
     */
    @Contract(pure = true)
    public Object getValue() {
        return this.value;
    }

    /**
     * @return the statically resolved node type
     */
    @Contract(pure = true)
    public NodeType<?> getType() {
        return this.type;
    }
}
