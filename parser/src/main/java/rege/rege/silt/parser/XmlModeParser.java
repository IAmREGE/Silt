package rege.rege.silt.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rege.rege.silt.base.ParseException;
import rege.rege.silt.base.ast.ConstantNode;
import rege.rege.silt.base.ast.Node;
import rege.rege.silt.xml.XmlMode;

/**
 * Parser transforming an {@link XmlMode} tree into an executable {@link Node}
 * AST using a pure Lisp-style S-expression tree model.
 * <p>
 * In this model, XML native types are strictly:
 * <ul>
 *   <li><b>Strings</b> (Text nodes): evaluated as string literals without
 *   magical type guessing</li>
 *   <li><b>Tags</b> (Element nodes): evaluated as Operator calls resolved from
 *   {@link OpRegistry}</li>
 * </ul>
 *
 * @param <C> evaluation context type
 * @author REGE
 * @since 0.0.1a1
 */
public class XmlModeParser<C> {
    /**
     * Default attribute name to specify operator override.
     */
    public static final String DEFAULT_OP_ATTR = "op";

    private final OpRegistry<C> opRegistry;
    private final LiteralTypeProvider typeProvider;
    private final String opAttr;

    /**
     * Constructs an XML parser with default operator attribute
     * {@value #DEFAULT_OP_ATTR}.
     *
     * @param opRegistry operator registry
     * @param typeProvider literal type provider
     */
    @Contract(pure = true)
    public XmlModeParser(
        @NotNull OpRegistry<C> opRegistry,
        @NotNull LiteralTypeProvider typeProvider
    ) {
        this(opRegistry, typeProvider, DEFAULT_OP_ATTR);
    }

    /**
     * Constructs an XML parser with custom operator attribute name.
     *
     * @param opRegistry operator registry
     * @param typeProvider literal type provider
     * @param opAttr custom operator attribute name (falls back to
     * {@value #DEFAULT_OP_ATTR} if null or empty)
     */
    @Contract(pure = true)
    public XmlModeParser(
        @NotNull OpRegistry<C> opRegistry,
        @NotNull LiteralTypeProvider typeProvider, @Nullable String opAttr
    ) {
        this.opRegistry = opRegistry;
        this.typeProvider = typeProvider;
        this.opAttr =
        (opAttr != null && opAttr.length() > 0) ? opAttr : DEFAULT_OP_ATTR;
    }

    /**
     * @return the associated operator registry
     */
    public OpRegistry<C> getOpRegistry() {
        return this.opRegistry;
    }

    /**
     * @return the associated literal type provider
     */
    public LiteralTypeProvider getTypeProvider() {
        return this.typeProvider;
    }

    /**
     * @return the configured operator attribute name
     */
    public String getOpAttr() {
        return this.opAttr;
    }

    /**
     * Recursively parses an {@link XmlMode} node into a typed {@link Node} AST.
     *
     * @param xml the XML view node to parse
     * @return parsed typed AST node
     * @throws ParseException if syntax, structure, or operator resolution fails
     */
    @NotNull
    public Node<C, ?> parse(@Nullable XmlMode xml) throws ParseException {
        if (xml == null) {
            throw new ParseException("XmlMode cannot be null");
        }

        // Case 1: Pure Text node -> String literal constant (No magical type guessing)
        String content = xml.getContent();
        if (content != null) {
            return new ConstantNode<C, String>(
                content.trim(), this.typeProvider.getStringType()
            );
        }

        // Case 2: Tag Element node -> Operator Call (Pure Lisp S-expression)
        String tagName = xml.getTagName();
        if (tagName == null) {
            throw new ParseException("Tag XmlMode returned null getTagName()");
        }

        Map<String, String> attrs = xml.getAttributes();
        String effectiveOpName = tagName;
        if (attrs != null && attrs.containsKey(this.opAttr)) {
            String override = attrs.get(this.opAttr);
            if (override != null && override.length() > 0) {
                effectiveOpName = override;
            }
        }

        List<Op<C>> candidates = this.opRegistry.getAll(effectiveOpName);
        if (candidates.isEmpty()) {
            throw new ParseException("Unknown operator: " + effectiveOpName);
        }

        boolean preserveSpace = this.isPreserveWhitespace(attrs);
        List<XmlMode> rawChildren = xml.getChildren();
        List<XmlMode> effectiveChildren = new ArrayList<XmlMode>();
        if (rawChildren != null) {
            for (XmlMode child : rawChildren) {
                if (child != null) {
                    String text = child.getContent();
                    // Inter-element whitespace: ignore pure formatting whitespace text nodes unless preserving space
                    if (!preserveSpace && text != null &&
                        text.trim().length() == 0) {
                        continue;
                    }
                    effectiveChildren.add(child);
                }
            }
        }

        // Branch 1: If child elements are present, evaluate them as Lisp positional argument sequence
        if (!effectiveChildren.isEmpty()) {
            if (effectiveChildren.size() == 1) {
                XmlMode singleChild = effectiveChildren.get(0);
                Node<C, ?> argNode;
                if (singleChild.getContent() != null && preserveSpace) {
                    argNode = new ConstantNode<C, String>(
                        singleChild.getContent(),
                        this.typeProvider.getStringType()
                    );
                } else {
                    argNode = this.parse(singleChild);
                }
                for (Op<C> candidate : candidates) {
                    if (candidate.acceptsSingleArg() &&
                        candidate.matchesSingleArg(argNode)) {
                        return candidate.createSingleArg(argNode);
                    }
                }
            }

            List<Node<C, ?>> childNodes =
            new ArrayList<Node<C, ?>>(effectiveChildren.size());
            for (XmlMode child : effectiveChildren) {
                if (child.getContent() != null && preserveSpace) {
                    childNodes.add(new ConstantNode<C, String>(
                        child.getContent(), this.typeProvider.getStringType()
                    ));
                } else {
                    childNodes.add(this.parse(child));
                }
            }

            for (Op<C> candidate : candidates) {
                if (candidate.acceptsPositionalArgs() &&
                    candidate.matchesPositionalArgs(childNodes)) {
                    return candidate.createPositional(childNodes);
                }
            }

            if (childNodes.size() == 1) {
                for (Op<C> candidate : candidates) {
                    if (candidate.acceptsSingleArg()) {
                        return candidate.createSingleArg(childNodes.get(0));
                    }
                }
            }

            throw new ParseException(
                "Operator '" + effectiveOpName +
                "' does not support positional children"
            );
        }

        // Branch 2: Leaf tag with no children -> check attributes for named args or single-arg shorthand
        if (attrs != null && !attrs.isEmpty()) {
            String singleVal = this.findSingleAttributeValue(attrs);
            if (singleVal != null) {
                Node<C, ?> argNode = new ConstantNode<C, String>(
                    singleVal, this.typeProvider.getStringType()
                );
                for (Op<C> candidate : candidates) {
                    if (candidate.acceptsSingleArg() &&
                        candidate.matchesSingleArg(argNode)) {
                        return candidate.createSingleArg(argNode);
                    }
                }
            }

            Map<String, Node<C, ?>> namedArgs = new HashMap<String, Node<C, ?>>();
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                String k = entry.getKey();
                if (!this.opAttr.equals(k) && !"xml:space".equals(k) &&
                    !"preserve".equals(k)) {
                    namedArgs.put(k, new ConstantNode<C, String>(
                        entry.getValue(), this.typeProvider.getStringType()
                    ));
                }
            }

            for (Op<C> candidate : candidates) {
                if (candidate.matchesNamedArgs(namedArgs)) {
                    return candidate.create(namedArgs);
                }
            }
        }

        // Branch 3: Empty tag <op/> with 0 arguments
        List<Node<C, ?>> emptyArgs = new ArrayList<Node<C, ?>>(0);
        for (Op<C> candidate : candidates) {
            if (candidate.acceptsPositionalArgs() &&
                candidate.matchesPositionalArgs(emptyArgs)) {
                return candidate.createPositional(emptyArgs);
            }
        }

        throw new ParseException(
            "Operator '" + effectiveOpName +
            "' requires arguments but none were provided"
        );
    }

    private boolean isPreserveWhitespace(Map<String, String> attrs) {
        return attrs != null && (
            "preserve".equals(attrs.get("xml:space")) ||
            "true".equalsIgnoreCase(attrs.get("preserve"))
        );
    }

    private String findSingleAttributeValue(Map<String, String> attrs) {
        if (attrs.containsKey("val")) {
            return attrs.get("val");
        }
        if (attrs.containsKey("value")) {
            return attrs.get("value");
        }
        String found = null;
        for (Map.Entry<String, String> e : attrs.entrySet()) {
            if (!this.opAttr.equals(e.getKey()) &&
                !"xml:space".equals(e.getKey()) &&
                !"preserve".equals(e.getKey())) {
                if (found != null) {
                    return null; // More than 1 candidate attribute
                }
                found = e.getValue();
            }
        }
        return found;
    }
}
