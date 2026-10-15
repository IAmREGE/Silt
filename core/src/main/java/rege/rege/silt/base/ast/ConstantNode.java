package rege.rege.silt.base.ast;

import org.jetbrains.annotations.Contract;
import rege.rege.silt.base.EvalException;

public final class ConstantNode<C, R> implements Node<C, R> {
    private final R value;
    private final NodeType<R> type;

    /**
     * Constructs a new {@code ConstantNode}.
     *
     * @param value the constant value held
     * @param type node type of the constant
     */
    @Contract(pure = true)
    public ConstantNode(R value, NodeType<R> type) {
        this.value = value;
        this.type = type;
    }

    @Contract(pure = true)
    public R apply(C context) throws EvalException {
        return this.value;
    }

    @Contract(pure = true)
    public NodeType<R> getType() {
        return this.type;
    }
}
