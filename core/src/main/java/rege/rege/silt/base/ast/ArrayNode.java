package rege.rege.silt.base.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rege.rege.silt.base.EvalException;

/**
 * An AST node evaluating a list of child element nodes and returning an
 * unmodifiable list of evaluated values.
 *
 * @param <C> evaluation context type
 * @param <E> list element type
 * @author REGE
 * @since 0.0.1a1
 */
public final class ArrayNode<C, E> implements Node<C, List<E>> {
    private final List<Node<C, E>> elements;
    private final NodeType<List<E>> type;

    /**
     * Constructs a new {@code ArrayNode}.
     *
     * @param elements list of child element nodes
     * @param type node type of the evaluated list
     */
    @Contract(pure = true)
    public ArrayNode(
        @NotNull List<Node<C, E>> elements, @NotNull NodeType<List<E>> type
    ) {
        this.elements =
        Collections.unmodifiableList(new ArrayList<Node<C, E>>(elements));
        this.type = type;
    }

    public List<E> apply(C context) throws EvalException {
        List<E> result = new ArrayList<E>(this.elements.size());
        for (Node<C, E> element : this.elements) {
            result.add(element.apply(context));
        }
        return result;
    }

    @Contract(pure = true)
    public NodeType<List<E>> getType() {
        return this.type;
    }

    @Contract(pure = true)
    public List<Node<C, E>> getElements() {
        return this.elements;
    }
}
