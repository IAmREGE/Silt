package rege.rege.silt.parser.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import rege.rege.silt.base.EvalException;
import rege.rege.silt.base.ParseException;
import rege.rege.silt.base.ast.ArrayNode;
import rege.rege.silt.base.ast.ConstantNode;
import rege.rege.silt.base.ast.Node;
import rege.rege.silt.base.ast.NodeType;
import rege.rege.silt.parser.AbstractOp;
import rege.rege.silt.parser.LiteralTypeProvider;
import rege.rege.silt.parser.OpRegistry;
import rege.rege.silt.parser.ParsedNumber;
import rege.rege.silt.parser.XmlModeParser;
import rege.rege.silt.xml.XmlMode;

public class XmlModeParserTest {

    // --- Custom NodeTypes for Testing ---

    private static final NodeType<String> STRING_TYPE =
    new SimpleNodeType<String>("String", String.class);
    private static final NodeType<Integer> INT_TYPE =
    new SimpleNodeType<Integer>("Int", Integer.class);
    private static final NodeType<Boolean> BOOL_TYPE =
    new SimpleNodeType<Boolean>("Bool", Boolean.class);
    private static final NodeType<Nil> NIL_TYPE =
    new SimpleNodeType<Nil>("Nil", Nil.class);
    @SuppressWarnings("unchecked,rawtypes")
    private static final NodeType<List<Object>> ARRAY_TYPE = (
        NodeType<List<Object>>
    )(NodeType<?>)new SimpleNodeType<List>("Array", List.class);

    // --- Test LiteralTypeProvider ---

    private static final LiteralTypeProvider TEST_TYPE_PROVIDER =
    new LiteralTypeProvider() {
        @NotNull
        @Contract(pure = true)
        public NodeType<String> getStringType() {
            return STRING_TYPE;
        }

        @NotNull
        @Contract(pure = true)
        public NodeType<Boolean> getBooleanType() {
            return BOOL_TYPE;
        }

        @Contract(pure = true)
        public Object getNullValue() {
            return Nil.INSTANCE;
        }

        @NotNull
        @Contract(pure = true)
        public NodeType<?> getNullType() {
            return NIL_TYPE;
        }

        @NotNull
        public ParsedNumber parseNumber(@NotNull String rawNumber)
        throws ParseException {
            try {
                return new ParsedNumber(Integer.valueOf(rawNumber), INT_TYPE);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid number: " + rawNumber, e);
            }
        }

        @NotNull
        @Contract(pure = true)
        public NodeType<List<Object>> getArrayType() {
            return ARRAY_TYPE;
        }
    };

    /**
     * Helper to create an OpRegistry configured with test operators.
     * In the pure Lisp model, even type converters like "int", "bool", "list" are regular registered Ops.
     */
    private static OpRegistry<Map<String, Object>> createStandardRegistry() {
        OpRegistry<Map<String, Object>> registry = new OpRegistry<Map<String, Object>>();

        // <int>123</int>
        registry.register(new AbstractOp<Map<String, Object>>("int", true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createSingleArg(@NotNull final Node<Map<String, Object>, ?> arg)
            throws ParseException {
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        Object val = arg.apply(context);
                        return Integer.valueOf(String.valueOf(val).trim());
                    }

                    @Contract(pure = true)
                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        // <str>hello</str>
        registry.register(new AbstractOp<Map<String, Object>>("str", true) {
            @Contract(value = "_ -> param1", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createSingleArg(@NotNull Node<Map<String, Object>, ?> arg)
            throws ParseException {
                return arg;
            }
        });

        // <bool>true</bool>
        registry.register(new AbstractOp<Map<String, Object>>("bool", true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createSingleArg(@NotNull final Node<Map<String, Object>, ?> arg)
            throws ParseException {
                return new Node<Map<String, Object>, Boolean>() {
                    public Boolean apply(Map<String, Object> context)
                    throws EvalException {
                        Object val = arg.apply(context);
                        return Boolean.valueOf(String.valueOf(val).trim());
                    }

                    @Contract(pure = true)
                    public NodeType<Boolean> getType() {
                        return BOOL_TYPE;
                    }
                };
            }
        });

        // <nil/>
        registry
        .register(new AbstractOp<Map<String, Object>>("nil", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new ConstantNode<Map<String, Object>, Nil>(
                    Nil.INSTANCE, NIL_TYPE
                );
            }
        });

        // <list>...</list>
        registry
        .register(new AbstractOp<Map<String, Object>>("list", false, true) {
            @SuppressWarnings("unchecked")
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                List<Node<Map<String, Object>, Object>> elements =
                new ArrayList<Node<Map<String, Object>, Object>>(args.size());
                for (Node<Map<String, Object>, ?> arg : args) {
                    elements.add((Node<Map<String, Object>, Object>)arg);
                }
                return new ArrayNode<Map<String, Object>, Object>(
                    elements, ARRAY_TYPE
                );
            }
        });

        return registry;
    }

    // --- Test Suite ---

    @Test
    public void testLispLiteralsViaOps() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        createStandardRegistry();
        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // <int>42</int>
        Node<Map<String, Object>, ?> intNode =
        parser.parse(MockXmlMode.tag("int", MockXmlMode.text("42")));
        Assert.assertEquals(Integer.valueOf(42), intNode.apply(null));
        Assert.assertEquals(INT_TYPE, intNode.getType());

        // <str>hello</str>
        Node<Map<String, Object>, ?> strNode =
        parser.parse(MockXmlMode.tag("str", MockXmlMode.text("hello")));
        Assert.assertEquals("hello", strNode.apply(null));
        Assert.assertEquals(STRING_TYPE, strNode.getType());

        // <bool>true</bool>
        Node<Map<String, Object>, ?> boolNode =
        parser.parse(MockXmlMode.tag("bool", MockXmlMode.text("true")));
        Assert.assertEquals(Boolean.TRUE, boolNode.apply(null));
        Assert.assertEquals(BOOL_TYPE, boolNode.getType());

        // <nil/>
        Node<Map<String, Object>, ?> nilNode =
        parser.parse(MockXmlMode.tag("nil"));
        Assert.assertSame(Nil.INSTANCE, nilNode.apply(null));
        Assert.assertEquals(NIL_TYPE, nilNode.getType());

        // <list><int>1</int><int>2</int></list>
        Node<Map<String, Object>, ?> listNode =
        parser.parse(MockXmlMode.tag(
            "list", MockXmlMode.tag("int", MockXmlMode.text("1")),
            MockXmlMode.tag("int", MockXmlMode.text("2"))
        ));
        Assert.assertTrue(listNode instanceof ArrayNode);
        Assert.assertEquals(Arrays.asList(1, 2), listNode.apply(null));
    }

    @Test
    public void testWhitespaceFormattingStripping() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();

        // Register Lisp-style positional "add"
        registry
        .register(new AbstractOp<Map<String, Object>>("add", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(final List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        int sum = 0;
                        for (Node<Map<String, Object>, ?> arg : args) {
                            Number n = (Number)arg.apply(context);
                            sum += n.intValue();
                        }
                        return Integer.valueOf(sum);
                    }

                    @Contract(pure = true)
                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // Formatted XML with indentation and line breaks:
        // <add>
        //     <int>
        //         10
        //     </int>
        //     <int>
        //         20
        //     </int>
        // </add>
        MockXmlMode formattedXml = MockXmlMode.tag(
            "add", MockXmlMode.text("\n    "),
            MockXmlMode.tag("int", MockXmlMode.text("\n        10\n    ")),
            MockXmlMode.text("\n    "),
            MockXmlMode.tag("int", MockXmlMode.text("\n        20\n    ")),
            MockXmlMode.text("\n")
        );

        Node<Map<String, Object>, ?> node = parser.parse(formattedXml);
        Assert.assertEquals(Integer.valueOf(30), node.apply(null));
    }

    @Test
    public void testSingleArgOpShorthand() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();

        // Register "getint": <getint>score</getint>
        registry.register(new AbstractOp<Map<String, Object>>("getint", true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createSingleArg(@NotNull final Node<Map<String, Object>, ?> arg)
            throws ParseException {
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        Object key = arg.apply(context);
                        return (Integer)context.get(String.valueOf(key));
                    }

                    @Contract(pure = true)
                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // <getint>score</getint>
        Node<Map<String, Object>, ?> node =
        parser.parse(MockXmlMode.tag("getint", MockXmlMode.text("score")));
        Assert.assertEquals(INT_TYPE, node.getType());

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("score", Integer.valueOf(99));

        Assert.assertEquals(Integer.valueOf(99), node.apply(ctx));
    }

    @Test
    public void testDuplicateSiblingTagsInLispTree() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();

        // <a><b/><b/></a> -> in Lisp: (a (b) (b))
        registry
        .register(new AbstractOp<Map<String, Object>>("b", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new ConstantNode<Map<String, Object>, String>(
                    "b_val", STRING_TYPE
                );
            }
        });

        registry
        .register(new AbstractOp<Map<String, Object>>("a", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(final List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        return Integer.valueOf(args.size());
                    }

                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // <a><b/><b/></a>
        MockXmlMode xml =
        MockXmlMode.tag("a", MockXmlMode.tag("b"), MockXmlMode.tag("b"));

        Node<Map<String, Object>, ?> node = parser.parse(xml);
        // "a" receives 2 positional child nodes: b and b!
        Assert.assertEquals(Integer.valueOf(2), node.apply(null));
    }

    @Test
    public void testMixedContentWithInterleavedTextAndTags()
    throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();

        // Register dummy Op "a"
        registry
        .register(new AbstractOp<Map<String, Object>>("a", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new Node<Map<String, Object>, String>() {
                    @Contract(pure = true)
                    public String apply(Map<String, Object> context)
                    throws EvalException {
                        return "a_val";
                    }

                    @Contract(pure = true)
                    public NodeType<String> getType() {
                        return STRING_TYPE;
                    }
                };
            }
        });

        // Register "root" which receives positional arguments
        registry
        .register(new AbstractOp<Map<String, Object>>("root", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(final List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new Node<Map<String, Object>, List<Object>>() {
                    @Contract("_ -> new")
                    public List<Object> apply(Map<String, Object> context)
                    throws EvalException {
                        List<Object> result =
                        new ArrayList<Object>(args.size());
                        for (Node<Map<String, Object>, ?> arg : args) {
                            result.add(arg.apply(context));
                        }
                        return result;
                    }

                    @Contract(pure = true)
                    public NodeType<List<Object>> getType() {
                        return ARRAY_TYPE;
                    }
                };
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // User's exact example: <root>1<a/>2<a/>3</root>
        MockXmlMode rootXml = MockXmlMode.tag(
            "root", MockXmlMode.text("1"), MockXmlMode.tag("a"),
            MockXmlMode.text("2"), MockXmlMode.tag("a"), MockXmlMode.text("3")
        );

        Node<Map<String, Object>, ?> node = parser.parse(rootXml);
        List<?> resultList = (List<?>) node.apply(null);

        Assert.assertEquals(5, resultList.size());
        Assert.assertEquals("1", resultList.get(0)); // Text node is pure String
        Assert.assertEquals("a_val", resultList.get(1));
        Assert.assertEquals("2", resultList.get(2));
        Assert.assertEquals("a_val", resultList.get(3));
        Assert.assertEquals("3", resultList.get(4));
    }

    @Test
    public void testPreserveWhitespaceAttribute() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();
        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put("xml:space", "preserve");

        // <str xml:space="preserve">  hello world  </str>
        MockXmlMode xml = MockXmlMode.tagWithAttrs(
            "str", attrs, MockXmlMode.text("  hello world  ")
        );

        Node<Map<String, Object>, ?> node = parser.parse(xml);
        Assert.assertEquals("  hello world  ", node.apply(null));
    }

    @Test
    public void testAttributesAsNamedArguments() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();

        // Register "sub" expecting "left" and "right" attributes
        registry.register(new AbstractOp<Map<String, Object>>("sub") {
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                if (!namedArgs.containsKey("left") ||
                    !namedArgs.containsKey("right")) {
                    throw new ParseException("Missing left or right");
                }
                final Node<Map<String, Object>, ?> left =
                namedArgs.get("left");
                final Node<Map<String, Object>, ?> right =
                namedArgs.get("right");

                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        return Integer.valueOf(Integer.parseInt(
                            String.valueOf(left.apply(context))
                        ) - Integer.parseInt(
                            String.valueOf(right.apply(context))
                        ));
                    }

                    @Contract(pure = true)
                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // <sub left="50" right="20"/>
        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put("left", "50");
        attrs.put("right", "20");

        MockXmlMode xml = MockXmlMode.tagWithAttrs("sub", attrs);
        Node<Map<String, Object>, ?> node = parser.parse(xml);
        Assert.assertEquals(Integer.valueOf(30), node.apply(null));
    }

    // --- ParseException Tests ---

    @Test(expected = ParseException.class)
    public void testParseNullThrowsParseException() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();
        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        parser.parse(null);
    }

    @Test(expected = ParseException.class)
    public void testUnknownOperatorThrowsParseException()
    throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();
        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        // <nonexistent_op>123</nonexistent_op>
        parser
        .parse(MockXmlMode.tag("nonexistent_op", MockXmlMode.text("123")));
    }

    @Test(expected = ParseException.class)
    public void testNestedUnknownOperatorThrowsParseException()
    throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();
        registry
        .register(new AbstractOp<Map<String, Object>>("add", false, true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createPositional(List<Node<Map<String, Object>, ?>> args)
            throws ParseException {
                return new ConstantNode<Map<String, Object>, Integer>(
                    0, INT_TYPE
                );
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        // <add><unknown_child/></add> -> child fails immediately! No swallowing!
        MockXmlMode xml =
        MockXmlMode.tag("add", MockXmlMode.tag("unknown_child"));
        parser.parse(xml);
    }

    @Test(expected = ParseException.class)
    public void testPositionalNotSupportedThrowsParseException()
    throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();
        // Register an Op that only accepts named args, not positional children
        registry.register(new AbstractOp<Map<String, Object>>("named_only") {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                return new ConstantNode<Map<String, Object>, Integer>(
                    1, INT_TYPE
                );
            }
        });

        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        // <named_only><int>1</int><int>2</int></named_only>
        MockXmlMode xml = MockXmlMode.tag(
            "named_only", MockXmlMode.tag("int", MockXmlMode.text("1")),
            MockXmlMode.tag("int", MockXmlMode.text("2"))
        );
        parser.parse(xml);
    }

    @Test(expected = ParseException.class)
    public void testNullTagNameThrowsParseException() throws ParseException {
        OpRegistry<Map<String, Object>> registry = createStandardRegistry();
        XmlModeParser<Map<String, Object>> parser =
        new XmlModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        // Tag node with null tagName and null content
        parser.parse(new XmlMode() {
            @Contract(value = "-> null", pure = true)
            public String getTagName() { return null; }
            @Contract(value = "-> null", pure = true)
            public Map<String, String> getAttributes() { return null; }
            @Contract(value = "-> null", pure = true)
            public String getContent() { return null; }
            @Contract(value = "-> null", pure = true)
            public List<XmlMode> getChildren() { return null; }
        });
    }
}
