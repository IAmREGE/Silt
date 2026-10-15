package rege.rege.silt.base.ast;

public interface NodeType<R> {
    boolean canAssignTo(NodeType<?> type);

    boolean accepts(R value);
}
