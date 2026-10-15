package rege.rege.silt.parser;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import rege.rege.silt.base.ParseException;
import rege.rege.silt.base.ast.Node;

/**
 * Represents an AST operator definition.
 * Supports pattern matching for operator overloading.
 *
 * @param <C> evaluation context type
 * @author REGE
 * @since 0.0.1a1
 */
public interface Op<C> {
    /**
     * @return the unique name of this operator
     */
    @NotNull
    String getName();

    /**
     * @return true if this operator can be invoked via single-argument
     * shorthand {@code { "name": arg }}
     */
    boolean acceptsSingleArg();

    /**
     * @return true if this operator can be invoked via positional argument
     * sequence
     */
    boolean acceptsPositionalArgs();

    /**
     * Checks if this operator overload matches the provided named arguments.
     *
     * @param namedArgs the parsed named argument nodes
     * @return true if this operator can handle the argument keys and types
     */
    boolean matchesNamedArgs(@NotNull Map<String, Node<C, ?>> namedArgs);

    /**
     * Checks if this operator overload matches the provided single argument.
     *
     * @param arg the parsed single argument node
     * @return true if this operator can handle the single argument
     */
    boolean matchesSingleArg(@NotNull Node<C, ?> arg);

    /**
     * Checks if this operator overload matches the provided positional arguments.
     *
     * @param args the parsed positional argument nodes
     * @return true if this operator can handle the positional arguments
     */
    boolean matchesPositionalArgs(@NotNull List<Node<C, ?>> args);

    /**
     * Constructs a node for single-argument shorthand invocation.
     *
     * @param arg the parsed single argument node
     * @return the resulting node
     * @throws ParseException if argument count or type is invalid
     */
    @NotNull
    Node<C, ?> createSingleArg(@NotNull Node<C, ?> arg) throws ParseException;

    /**
     * Constructs a node from positional argument sequence.
     *
     * @param args the parsed positional argument nodes
     * @return the resulting node
     * @throws ParseException if argument count or type is invalid
     */
    @NotNull
    Node<C, ?> createPositional(@NotNull List<Node<C, ?>> args)
    throws ParseException;

    /**
     * Constructs a node from named key-value mapping arguments.
     *
     * @param namedArgs the parsed named argument nodes
     * @return the resulting node
     * @throws ParseException if required arguments are missing or invalid
     */
    @NotNull
    Node<C, ?> create(@NotNull Map<String, Node<C, ?>> namedArgs)
    throws ParseException;
}
