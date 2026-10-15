package rege.rege.silt.parser;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rege.rege.silt.base.ParseException;
import rege.rege.silt.base.ast.NodeType;

/**
 * Provider interface for literal {@link NodeType} definitions and number
 * parsing.
 *
 * @author REGE
 * @since 0.0.1a1
 */
public interface LiteralTypeProvider {
    /**
     * @return node type for string literals
     */
    @NotNull
    NodeType<String> getStringType();

    /**
     * @return node type for boolean literals
     */
    @NotNull
    NodeType<Boolean> getBooleanType();

    /**
     * @return singleton value for null literals (e.g. Nil.INSTANCE or null)
     */
    @Nullable
    Object getNullValue();

    /**
     * @return node type for null literals
     */
    @NotNull
    NodeType<?> getNullType();

    /**
     * Parses a raw number string into a {@link ParsedNumber}.
     *
     * @param rawNumber exact text representation of the number
     * @return parsed number container
     * @throws ParseException if the number string is invalid
     */
    @NotNull
    ParsedNumber parseNumber(@NotNull String rawNumber) throws ParseException;

    /**
     * @return node type for array literals
     */
    @NotNull
    NodeType<List<Object>> getArrayType();
}
