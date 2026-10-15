package rege.rege.silt.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rege.rege.silt.base.ParseException;
import rege.rege.silt.base.ast.ArrayNode;
import rege.rege.silt.base.ast.ConstantNode;
import rege.rege.silt.base.ast.Node;
import rege.rege.silt.base.ast.NodeType;
import rege.rege.silt.json.JsonMode;

/**
 * Parser transforming a {@link JsonMode} tree into an executable {@link Node}
 * AST. Supports configurable operator key and single-argument shorthand syntax.
 *
 * @param <C> evaluation context type
 * @author REGE
 * @since 0.0.1a1
 */
public class JsonModeParser<C> {
    /**
     * Default operator keyword key name in JSON mappings.
     */
    public static final String DEFAULT_OP_KEY = "op";

    @NotNull private final OpRegistry<C> opRegistry;
    @NotNull private final LiteralTypeProvider typeProvider;
    @NotNull private final String opKey;

    /**
     * Constructs a parser with the default operator key
     * {@value #DEFAULT_OP_KEY}.
     *
     * @param opRegistry operator registry
     * @param typeProvider literal type provider
     */
    @Contract(pure = true)
    public JsonModeParser(
        @NotNull OpRegistry<C> opRegistry,
        @NotNull LiteralTypeProvider typeProvider
    ) {
        this(opRegistry, typeProvider, DEFAULT_OP_KEY);
    }

    /**
     * Constructs a parser with a custom operator key name.
     *
     * @param opRegistry operator registry
     * @param typeProvider literal type provider
     * @param opKey custom operator key name (falls back to
     * {@value #DEFAULT_OP_KEY} if null)
     */
    @Contract(pure = true)
    public JsonModeParser(
        @NotNull OpRegistry<C> opRegistry,
        @NotNull LiteralTypeProvider typeProvider, @Nullable String opKey
    ) {
        this.opRegistry = opRegistry;
        this.typeProvider = typeProvider;
        this.opKey = opKey != null ? opKey : DEFAULT_OP_KEY;
    }

    /**
     * @return the configured operator keyword key name
     */
    @NotNull
    public String getOpKey() {
        return this.opKey;
    }

    /**
     * @return the associated operator registry
     */
    @NotNull
    public OpRegistry<C> getOpRegistry() {
        return this.opRegistry;
    }

    /**
     * @return the associated literal type provider
     */
    @NotNull
    public LiteralTypeProvider getTypeProvider() {
        return this.typeProvider;
    }

    /**
     * Recursively parses a {@link JsonMode} node into a typed {@link Node} AST.
     *
     * @param json the JSON view node to parse
     * @return parsed typed AST node
     * @throws ParseException if syntax, structure, or operator resolution fails
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public Node<C, ?> parse(@Nullable JsonMode json) throws ParseException {
        if (json == null) {
            throw new ParseException("JsonMode cannot be null");
        }

        JsonMode.Type type = json.getType();
        if (type == JsonMode.Type.STRING) {
            return new ConstantNode<C, String>(
                json.getPrimitive(), this.typeProvider.getStringType()
            );
        } else if (type == JsonMode.Type.BOOLEAN) {
            return new ConstantNode<C, Boolean>(
                Boolean.valueOf(json.getPrimitive()),
                this.typeProvider.getBooleanType()
            );
        } else if (type == JsonMode.Type.NUMBER) {
            ParsedNumber num =
            this.typeProvider.parseNumber(json.getPrimitive());
            return new ConstantNode<C, Object>(
                num.getValue(), (NodeType<Object>)num.getType()
            );
        } else if (type == JsonMode.Type.NULL) {
            return new ConstantNode<C, Object>(
                this.typeProvider.getNullValue(),
                (NodeType<Object>)this.typeProvider.getNullType()
            );
        } else if (type == JsonMode.Type.ARRAY) {
            List<JsonMode> rawList = json.getArray();
            if (rawList == null) {
                throw new ParseException(
                    "Array JsonMode returned null getArray()"
                );
            }
            List<Node<C, Object>> elements =
            new ArrayList<Node<C, Object>>(rawList.size());
            for (JsonMode jsonMode : rawList) {
                elements.add((Node<C, Object>)this.parse(jsonMode));
            }
            return new ArrayNode<C, Object>(
                elements, this.typeProvider.getArrayType()
            );
        } else if (type == JsonMode.Type.OBJECT) {
            Map<String, JsonMode> map = json.getObject();
            if (map == null) {
                throw new ParseException(
                    "Object JsonMode returned null getObject()"
                );
            }

            // 1. Check if mapping contains the configured opKey
            if (map.containsKey(this.opKey)) {
                JsonMode opMode = map.get(this.opKey);
                if (opMode == null ||
                    opMode.getType() != JsonMode.Type.STRING ||
                    opMode.getPrimitive() == null) {
                    throw new ParseException(
                        "Operator key \"" +
                        this.opKey.replace("\\", "\\\\").replace("\"", "\\\"")
                        + "\" must be a string literal"
                    );
                }
                String opName = opMode.getPrimitive();
                List<Op<C>> candidates = this.opRegistry.getAll(opName);
                if (candidates.isEmpty()) {
                    throw new ParseException("Unknown operator: " + opName);
                }

                Map<String, Node<C, ?>> namedArgs =
                new HashMap<String, Node<C, ?>>();
                for (Map.Entry<String, JsonMode> entry : map.entrySet()) {
                    String k = entry.getKey();
                    if (!this.opKey.equals(k)) {
                        namedArgs.put(k, this.parse(entry.getValue()));
                    }
                }

                List<Op<C>> matched = new ArrayList<Op<C>>();
                for (Op<C> candidate : candidates) {
                    if (candidate.matchesNamedArgs(namedArgs)) {
                        matched.add(candidate);
                    }
                }

                if (matched.size() == 1) {
                    return matched.get(0).create(namedArgs);
                } else if (matched.size() > 1) {
                    throw new ParseException(
                        "Ambiguous overloads for operator '" + opName +
                        "' matching arguments " + namedArgs.keySet()
                    );
                } else {
                    throw new ParseException(
                        "No matching overload for operator '" + opName +
                        "' with arguments " + namedArgs.keySet()
                    );
                }
            }

            // 2. Check if mapping is a single-key object matching a single-argument Op
            if (map.size() == 1) {
                Map.Entry<String, JsonMode> singleEntry =
                map.entrySet().iterator().next();
                String candidateName = singleEntry.getKey();
                List<Op<C>> candidates = this.opRegistry.getAll(candidateName);
                if (!candidates.isEmpty()) {
                    Node<C, ?> argNode = this.parse(singleEntry.getValue());
                    for (Op<C> candidate : candidates) {
                        if (candidate.acceptsSingleArg() &&
                            candidate.matchesSingleArg(argNode)) {
                            return candidate.createSingleArg(argNode);
                        }
                    }
                }
            }

            throw new ParseException(
                "Unrecognized object expression: missing \"" +
                this.opKey.replace("\\", "\\\\").replace("\"", "\\\"") +
                "\" or valid single-arg operator in " + map.keySet()
            );
        } else {
            throw new ParseException("Unsupported JsonMode.Type: " + type);
        }
    }
}
