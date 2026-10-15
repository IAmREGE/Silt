package rege.rege.silt.base.ast;

import rege.rege.silt.base.EvalException;

public interface Node<C, R> {
    R apply(C context) throws EvalException;

    NodeType<R> getType();
}
