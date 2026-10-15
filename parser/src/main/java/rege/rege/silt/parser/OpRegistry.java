package rege.rege.silt.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registry maintaining operator definitions for AST parsing.
 * Supports registering multiple overloads under the same operator name.
 *
 * @param <C> evaluation context type
 * @author REGE
 * @since 0.0.1a1
 */
public class OpRegistry<C> {
    private final Map<String, List<Op<C>>> ops;

    /**
     * Constructs a new empty {@code OpRegistry}.
     */
    public OpRegistry() {
        this.ops = new HashMap<String, List<Op<C>>>();
    }

    /**
     * Registers an operator overload. Multiple operators with the same name
     * can be registered.
     *
     * @param op the operator to register
     */
    public void register(@NotNull Op<C> op) {
        String name = op.getName();
        List<Op<C>> list = this.ops.get(name);
        if (list == null) {
            list = new ArrayList<Op<C>>();
            this.ops.put(name, list);
        }
        list.add(op);
    }

    /**
     * Finds the first registered operator by name, or null if none registered.
     *
     * @param name operator name
     * @return the first registered operator, or null if not found
     */
    @Nullable
    public Op<C> get(@NotNull String name) {
        List<Op<C>> list = this.ops.get(name);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Returns all registered overloads for an operator name.
     *
     * @param name operator name
     * @return list of registered operator overloads (empty list if not found)
     */
    @NotNull
    public List<Op<C>> getAll(@NotNull String name) {
        List<Op<C>> list = this.ops.get(name);
        if (list != null) {
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * Checks if an operator is registered.
     *
     * @param name operator name
     * @return true if at least one operator overload is registered
     */
    public boolean contains(@NotNull String name) {
        List<Op<C>> list = this.ops.get(name);
        return list != null && !list.isEmpty();
    }
}
