package rege.rege.silt.parser.test;

import org.jetbrains.annotations.Contract;
import rege.rege.silt.base.ast.NodeType;

public class SimpleNodeType<T> implements NodeType<T> {
    private final String name;
    private final Class<T> clazz;

    @Contract(pure = true)
    public SimpleNodeType(String name, Class<T> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public boolean canAssignTo(NodeType<?> type) {
        return (type instanceof SimpleNodeType) &&
               ((SimpleNodeType<?>)type).clazz.isAssignableFrom(this.clazz);
    }

    public boolean accepts(T value) {
        return this.clazz.isInstance(value);
    }

    public String toString() {
        return this.name;
    }
}
