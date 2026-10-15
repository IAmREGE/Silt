package rege.rege.silt.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rege.rege.silt.base.ParseException;
import rege.rege.silt.base.ast.Node;

/**
 * Abstract base class for {@link Op} implementations.
 * Supports overload matching by argument keys, arity, and argument types.
 *
 * @param <C> evaluation context type
 * @author REGE
 * @since 0.0.1a1
 */
public abstract class AbstractOp<C> implements Op<C> {
    @NotNull private final String name;
    private final boolean acceptsSingleArg;
    private final boolean acceptsPositionalArgs;
    private final boolean acceptsNamedArgs;
    @Nullable private final Set<String> requiredNamedArgs;
    private final int expectedPositionalCount;

    /**
     * Constructs an {@code AbstractOp} with complete configuration.
     *
     * @param name operator name
     * @param acceptsSingleArg whether single-arg shorthand is supported
     * @param acceptsPositionalArgs whether positional arguments are supported
     * @param acceptsNamedArgs whether named arguments mapping is supported
     * @param requiredNamedArgs exact set of argument names required, or null
     * for any
     * @param expectedPositionalCount expected positional argument count, or -1
     * for any
     */
    @Contract(pure = true)
    public AbstractOp(
        @NotNull String name, boolean acceptsSingleArg,
        boolean acceptsPositionalArgs, boolean acceptsNamedArgs,
        @Nullable Set<String> requiredNamedArgs, int expectedPositionalCount
    ) {
        this.name = name;
        this.acceptsSingleArg = acceptsSingleArg;
        this.acceptsPositionalArgs = acceptsPositionalArgs;
        this.acceptsNamedArgs = acceptsNamedArgs;
        this.requiredNamedArgs = requiredNamedArgs;
        this.expectedPositionalCount = expectedPositionalCount;
    }

    /**
     * Constructs an {@code AbstractOp} with explicit single-arg, positional,
     * and named-args flags.
     *
     * @param name operator name
     * @param acceptsSingleArg whether single-arg shorthand is supported
     * @param acceptsPositionalArgs whether positional arguments are supported
     * @param acceptsNamedArgs whether named arguments mapping is supported
     */
    @Contract(pure = true)
    public AbstractOp(
        @NotNull String name, boolean acceptsSingleArg,
        boolean acceptsPositionalArgs, boolean acceptsNamedArgs
    ) {
        this(name, acceptsSingleArg, acceptsPositionalArgs, acceptsNamedArgs,
             null, -1);
    }

    /**
     * Constructs an {@code AbstractOp} with positional args flag.
     *
     * @param name operator name
     * @param acceptsSingleArg whether single-arg shorthand is supported
     * @param acceptsPositionalArgs whether positional arguments are supported
     */
    @Contract(pure = true)
    public AbstractOp(
        @NotNull String name, boolean acceptsSingleArg,
        boolean acceptsPositionalArgs
    ) {
        this(name, acceptsSingleArg, acceptsPositionalArgs, true, null, -1);
    }

    /**
     * Constructs an {@code AbstractOp} matching exact named argument keys.
     *
     * @param name operator name
     * @param requiredNamedArgs exact argument keys this overload accepts
     */
    @Contract(pure = true)
    public
    AbstractOp(@NotNull String name, @NotNull String[] requiredNamedArgs) {
        this(name, false, false, true,
             new HashSet<String>(Arrays.asList(requiredNamedArgs)), -1);
    }

    /**
     * Constructs an {@code AbstractOp} supporting single-arg shorthand and
     * exact named argument keys.
     *
     * @param name operator name
     * @param acceptsSingleArg whether single-arg shorthand is supported
     * @param requiredNamedArgs exact argument keys this overload accepts
     */
    @Contract(pure = true)
    public AbstractOp(
        @NotNull String name, boolean acceptsSingleArg,
        @NotNull String[] requiredNamedArgs
    ) {
        this(name, acceptsSingleArg, false, true,
             new HashSet<String>(Arrays.asList(requiredNamedArgs)), -1);
    }

    /**
     * Constructs an {@code AbstractOp} with single-arg configuration.
     * If {@code acceptsSingleArg} is true, named-args is disabled by default
     * to avoid accidental collision with multi-arg overloads.
     *
     * @param name operator name
     * @param acceptsSingleArg whether single-arg shorthand is supported
     */
    @Contract(pure = true)
    public AbstractOp(@NotNull String name, boolean acceptsSingleArg) {
        this(name, acceptsSingleArg, false, !acceptsSingleArg, null, -1);
    }

    /**
     * Constructs an {@code AbstractOp} with default named-arguments support.
     *
     * @param name operator name
     */
    @Contract(pure = true)
    public AbstractOp(@NotNull String name) {
        this(name, false, false, true, null, -1);
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public boolean acceptsSingleArg() {
        return this.acceptsSingleArg;
    }

    public boolean acceptsPositionalArgs() {
        return this.acceptsPositionalArgs;
    }

    public boolean acceptsNamedArgs() {
        return this.acceptsNamedArgs;
    }

    @Nullable
    public Set<String> getRequiredNamedArgs() {
        return this.requiredNamedArgs;
    }

    public boolean matchesNamedArgs(@NotNull Map<String, Node<C, ?>> namedArgs) {
        if (!this.acceptsNamedArgs) {
            return false;
        }
        if (this.requiredNamedArgs != null) {
            return namedArgs.keySet().equals(this.requiredNamedArgs);
        }
        return true;
    }

    public boolean matchesSingleArg(@NotNull Node<C, ?> arg) {
        return this.acceptsSingleArg;
    }

    public boolean matchesPositionalArgs(@NotNull List<Node<C, ?>> args) {
        if (!this.acceptsPositionalArgs) {
            return false;
        }
        if (this.expectedPositionalCount >= 0) {
            return args.size() == this.expectedPositionalCount;
        }
        return true;
    }

    @NotNull
    public Node<C, ?> createSingleArg(@NotNull Node<C, ?> arg)
    throws ParseException {
        throw new ParseException(
            "Operator \"" +
            this.name.replace("\\", "\\\\").replace("\"", "\\\"") +
            "\" does not support single-argument shorthand"
        );
    }

    @NotNull
    public Node<C, ?> createPositional(@Nullable List<Node<C, ?>> args)
    throws ParseException {
        if (args != null && args.size() == 1 && this.acceptsSingleArg()) {
            return this.createSingleArg(args.get(0));
        }
        throw new ParseException(
            "Operator '" +
            this.name.replace("\\", "\\\\").replace("\"", "\\\"") +
            "' does not support positional arguments"
        );
    }

    @NotNull
    public Node<C, ?> create(@NotNull Map<String, Node<C, ?>> namedArgs)
    throws ParseException {
        throw new ParseException(
            "Operator '" +
            this.name.replace("\\", "\\\\").replace("\"", "\\\"") +
            "' does not support named-arguments mapping"
        );
    }
}
