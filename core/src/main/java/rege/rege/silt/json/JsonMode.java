package rege.rege.silt.json;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A view of a JSON value.
 * @author REGE
 * @since 0.0.1a1
 */
public interface JsonMode {
    @NotNull
    Type getType();

    /**
     * For primitive values, return the literals as-is from the underlying JSON
     * source.
     * <p>
     * For NUMBER type, this returns the exact textual representation as it
     * appeared in the original JSON, preserving all digits and decimal places
     * without any parsing or precision loss. The consumer is responsible for
     * parsing this string appropriately (e.g., BigDecimal, BigInteger, Double,
     * etc.).
     * <p>
     * For value of other types, return {@code null}.
     */
    @Nullable
    String getPrimitive();

    /**
     * For array values, return the lists of their elements; for value of other
     * types, return {@code null}.
     */
    @Nullable
    List<JsonMode> getArray();

    /**
     * For object values, return the maps of their key-value pairs; for value
     * of other types, return {@code null}.
     */
    @Nullable
    Map<String, JsonMode> getObject();

    /**
     * @author REGE
     * @since 0.0.1a1
     */
    public static enum Type {
        NULL,
        STRING,
        NUMBER,
        BOOLEAN,
        ARRAY,
        OBJECT;
    }
}
