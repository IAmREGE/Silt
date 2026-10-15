package rege.rege.silt.parser.test;

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
import rege.rege.silt.json.JsonMode;
import rege.rege.silt.parser.AbstractOp;
import rege.rege.silt.parser.JsonModeParser;
import rege.rege.silt.parser.LiteralTypeProvider;
import rege.rege.silt.parser.OpRegistry;
import rege.rege.silt.parser.ParsedNumber;

public class JsonModeParserTest {

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

    // --- Test Suite ---

    @Test
    public void testParsePrimitivesAndArrayLiteral() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // String
        Node<Map<String, Object>, ?> strNode =
        parser.parse(MockJsonMode.string("hello"));
        Assert.assertEquals("hello", strNode.apply(null));
        Assert.assertEquals(STRING_TYPE, strNode.getType());

        // Number
        Node<Map<String, Object>, ?> numNode =
        parser.parse(MockJsonMode.number("42"));
        Assert.assertEquals(Integer.valueOf(42), numNode.apply(null));
        Assert.assertEquals(INT_TYPE, numNode.getType());

        // Null -> Nil singleton!
        Node<Map<String, Object>, ?> nullNode =
        parser.parse(MockJsonMode.nullVal());
        Assert.assertSame(Nil.INSTANCE, nullNode.apply(null));
        Assert.assertEquals(NIL_TYPE, nullNode.getType());

        // Array literal
        Node<Map<String, Object>, ?> arrNode =
        parser.parse(MockJsonMode.array(new JsonMode[]{
            MockJsonMode.number("1"), MockJsonMode.number("2"),
            MockJsonMode.number("3")
        }));
        Assert.assertTrue(arrNode instanceof ArrayNode);
        Object evaluatedList = arrNode.apply(null);
        Assert.assertEquals(Arrays.asList(1, 2, 3), evaluatedList);
    }

    @Test
    public void testSingleArgOpShorthand() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();

        // Register "getint": single-argument op taking variable name
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

        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // { "getint": "score" }
        Map<String, JsonMode> shorthandMap = new HashMap<String, JsonMode>();
        shorthandMap.put("getint", MockJsonMode.string("score"));

        Node<Map<String, Object>, ?> node =
        parser.parse(MockJsonMode.object(shorthandMap));
        Assert.assertEquals(INT_TYPE, node.getType());

        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("score", Integer.valueOf(100));

        Object result = node.apply(ctx);
        Assert.assertEquals(Integer.valueOf(100), result);
    }

    @Test
    public void testDualModeMinusOperator() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();

        // Overload 1: Unary negation with single-argument shorthand {"-": 15}
        registry.register(new AbstractOp<Map<String, Object>>("-", true) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?>
            createSingleArg(@NotNull final Node<Map<String, Object>, ?> arg)
            throws ParseException {
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        Number n = (Number)arg.apply(context);
                        return Integer.valueOf(-n.intValue());
                    }

                    @Contract(pure = true)
                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        // Overload 2: Binary subtraction with named arguments {"op": "-", "left": ..., "right": ...}
        registry.register(new AbstractOp<Map<String, Object>>("-", new String[]{"left", "right"}) {
            @Contract("_ -> new")
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                final Node<Map<String, Object>, ?> left = namedArgs.get("left");
                final Node<Map<String, Object>, ?> right = namedArgs.get("right");

                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        Number l = (Number)left.apply(context);
                        Number r = (Number)right.apply(context);
                        return Integer.valueOf(l.intValue() - r.intValue());
                    }

                    @Contract(pure = true)
                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // 1. Unary negation shorthand: { "-": 15 } -> matches Overload 1!
        Map<String, JsonMode> unaryMap = new HashMap<String, JsonMode>();
        unaryMap.put("-", MockJsonMode.number("15"));
        Node<Map<String, Object>, ?> unaryNode =
        parser.parse(MockJsonMode.object(unaryMap));
        Assert.assertEquals(Integer.valueOf(-15), unaryNode.apply(null));

        // 2. Binary subtraction: { "op": "-", "left": 100, "right": 25 } -> matches Overload 2!
        Map<String, JsonMode> binaryMap = new HashMap<String, JsonMode>();
        binaryMap.put("op", MockJsonMode.string("-"));
        binaryMap.put("left", MockJsonMode.number("100"));
        binaryMap.put("right", MockJsonMode.number("25"));
        Node<Map<String, Object>, ?> binaryNode =
        parser.parse(MockJsonMode.object(binaryMap));
        Assert.assertEquals(Integer.valueOf(75), binaryNode.apply(null));
    }

    @Test
    public void testPythonPowOperatorOverload() throws ParseException {
        OpRegistry<Map<String, Object>> registry = new OpRegistry<Map<String, Object>>();

        // Overload 1: pow(left, right) -> left^right
        registry.register(new AbstractOp<Map<String, Object>>(
            "pow", new String[]{"left", "right"}
        ) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                final Node<Map<String, Object>, ?> left =
                namedArgs.get("left");
                final Node<Map<String, Object>, ?> right =
                namedArgs.get("right");
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        int base = ((Number)left.apply(context)).intValue();
                        int exp = ((Number)right.apply(context)).intValue();
                        int result = 1;
                        for (int i = 0; i < exp; i++) {
                            result *= base;
                        }
                        return Integer.valueOf(result);
                    }

                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        // Overload 2: pow(base, exp, mod) -> (base^exp) % mod
        registry.register(new AbstractOp<Map<String, Object>>(
            "pow", new String[]{"base", "exp", "mod"}
        ) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                final Node<Map<String, Object>, ?> base= namedArgs.get("base");
                final Node<Map<String, Object>, ?> exp = namedArgs.get("exp");
                final Node<Map<String, Object>, ?> mod = namedArgs.get("mod");
                return new Node<Map<String, Object>, Integer>() {
                    public Integer apply(Map<String, Object> context)
                    throws EvalException {
                        int b = ((Number)base.apply(context)).intValue();
                        int e = ((Number)exp.apply(context)).intValue();
                        int m = ((Number)mod.apply(context)).intValue();
                        int result = 1;
                        for (int i = 0; i < e; i++) {
                            result = (result * b) % m;
                        }
                        return Integer.valueOf(result);
                    }

                    public NodeType<Integer> getType() {
                        return INT_TYPE;
                    }
                };
            }
        });

        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // 1. Binary pow: {"op": "pow", "left": 2, "right": 3} -> 2^3 = 8
        Map<String, JsonMode> binaryMap = new HashMap<String, JsonMode>();
        binaryMap.put("op", MockJsonMode.string("pow"));
        binaryMap.put("left", MockJsonMode.number("2"));
        binaryMap.put("right", MockJsonMode.number("3"));

        Node<Map<String, Object>, ?> node1 =
        parser.parse(MockJsonMode.object(binaryMap));
        Assert.assertEquals(Integer.valueOf(8), node1.apply(null));

        // 2. Ternary pow: {"op": "pow", "base": 2, "exp": 3, "mod": 5} -> (2^3) % 5 = 8 % 5 = 3
        Map<String, JsonMode> ternaryMap = new HashMap<String, JsonMode>();
        ternaryMap.put("op", MockJsonMode.string("pow"));
        ternaryMap.put("base", MockJsonMode.number("2"));
        ternaryMap.put("exp", MockJsonMode.number("3"));
        ternaryMap.put("mod", MockJsonMode.number("5"));

        Node<Map<String, Object>, ?> node2 =
        parser.parse(MockJsonMode.object(ternaryMap));
        Assert.assertEquals(Integer.valueOf(3), node2.apply(null));
    }

    @Test
    public void testCustomOpKey() throws ParseException {
        OpRegistry<Map<String, Object>> registry = new OpRegistry<Map<String, Object>>();
        registry.register(new AbstractOp<Map<String, Object>>("ping") {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                return new ConstantNode<Map<String, Object>, String>(
                    "pong", STRING_TYPE
                );
            }
        });

        // Configured with custom opKey "$action" instead of "op"
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(
            registry, TEST_TYPE_PROVIDER, "$action"
        );
        Assert.assertEquals("$action", parser.getOpKey());

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("$action", MockJsonMode.string("ping"));

        Node<Map<String, Object>, ?> node =
        parser.parse(MockJsonMode.object(map));
        Assert.assertEquals("pong", node.apply(null));
    }

    // --- ParseException Tests ---

    // 1. nonexistent op (no pattern matched)

    @Test(expected = ParseException.class)
    public void testUnknownOperatorWithOpKeyThrowsParseException()
    throws ParseException {
        // Explicit "op" key with unregistered operator
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("op", MockJsonMode.string("nonexistent_op"));

        parser.parse(MockJsonMode.object(map));
    }

    @Test(expected = ParseException.class)
    public void testSingleArgPatternOpNotFoundThrowsParseException()
    throws ParseException {
        // Single key-value pair, but the key is not a registered operator (cannot match single-arg pattern)
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("nonexistent_single_op", MockJsonMode.number("42"));

        parser.parse(MockJsonMode.object(map));
    }

    @Test(expected = ParseException.class)
    public void
    testSingleArgPatternOpDoesNotAcceptSingleArgThrowsParseException()
    throws ParseException {
        // Single key-value pair matching a registered operator, but that operator does NOT accept single-arg shorthand
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        registry
        .register(new AbstractOp<Map<String, Object>>("multi_only", false) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                return new ConstantNode<Map<String, Object>, Integer>(
                    0, INT_TYPE
                );
            }
        });

        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("multi_only", MockJsonMode.number("42"));

        parser.parse(MockJsonMode.object(map));
    }

    @Contract("-> new")
    public static OpRegistry<Map<String, Object>> powRegistry() {
        // pow has two overloads: (left, right) and (base, exp, mod). Passing { "op": "pow", "foo": 1 } matches neither!
        OpRegistry<Map<String, Object>> registry = new OpRegistry<Map<String, Object>>();
        registry.register(new AbstractOp<Map<String, Object>>(
            "pow", new String[]{"left", "right"}
        ) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) {
                return new ConstantNode<Map<String, Object>, Integer>(
                    0, INT_TYPE
                );
            }
        });
        registry.register(new AbstractOp<Map<String, Object>>(
            "pow", new String[]{"base", "exp", "mod"}
        ) {
            @Contract(value = "_ -> new", pure = true)
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) {
                return new ConstantNode<Map<String, Object>, Integer>(
                    0, INT_TYPE
                );
            }
        });
        return registry;
    }

    @Test(expected = ParseException.class)
    public void testNoMatchingOverloadThrowsParseException1()
    throws ParseException {
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(powRegistry(), TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("op", MockJsonMode.string("pow"));
        map.put("foo", MockJsonMode.number("1"));

        parser.parse(MockJsonMode.object(map));
    }

    @Test(expected = ParseException.class)
    public void testNoMatchingOverloadThrowsParseException2()
    throws ParseException {
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(powRegistry(), TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("op", MockJsonMode.string("pow"));
        map.put("left", MockJsonMode.number("1"));

        parser.parse(MockJsonMode.object(map));
    }

    @Test(expected = ParseException.class)
    public void testNoMatchingOverloadThrowsParseException3()
    throws ParseException {
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(powRegistry(), TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("op", MockJsonMode.string("pow"));
        map.put("left", MockJsonMode.number("1"));
        map.put("right", MockJsonMode.number("2"));
        map.put("base", MockJsonMode.number("1"));

        parser.parse(MockJsonMode.object(map));
    }

    // 2. op 实参类型错误 (Static type mismatch at parse time)

    @Test(expected = ParseException.class)
    public void testSingleArgOpTypeMismatchThrowsParseException()
    throws ParseException {
        // Operator expecting INT_TYPE receives STRING_TYPE argument
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        registry.register(new AbstractOp<Map<String, Object>>("negate", true) {
            @NotNull
            public Node<Map<String, Object>, ?>
            createSingleArg(@NotNull Node<Map<String, Object>, ?> arg)
                throws ParseException {
                if (!arg.getType().canAssignTo(INT_TYPE)) {
                    throw new ParseException(
                        "Operator 'negate' expects Integer, but got: " +
                        arg.getType()
                    );
                }
                return new ConstantNode<Map<String, Object>, Integer>(
                    1, INT_TYPE
                );
            }
        });

        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // { "negate": "not_a_number" }
        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("negate", MockJsonMode.string("not_a_number"));

        parser.parse(MockJsonMode.object(map));
    }

    @Test(expected = ParseException.class)
    public void testNamedArgsOpTypeMismatchThrowsParseException()
    throws ParseException {
        // Operator expecting INT_TYPE for "left" and "right" receives Boolean for "right"
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        registry.register(new AbstractOp<Map<String, Object>>("sub") {
            @NotNull
            public Node<Map<String, Object>, ?> create(
                @NotNull Map<String, Node<Map<String, Object>, ?>> namedArgs
            ) throws ParseException {
                Node<Map<String, Object>, ?> left = namedArgs.get("left");
                Node<Map<String, Object>, ?> right = namedArgs.get("right");
                if (left == null || !left.getType().canAssignTo(INT_TYPE)) {
                    throw new ParseException("'left' must be Integer");
                }
                if (right == null || !right.getType().canAssignTo(INT_TYPE)) {
                    throw new ParseException("'right' must be Integer");
                }
                return new ConstantNode<Map<String, Object>, Integer>(
                    0, INT_TYPE
                );
            }
        });

        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        // { "op": "sub", "left": 100, "right": true }
        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("op", MockJsonMode.string("sub"));
        map.put("left", MockJsonMode.number("100"));
        map.put("right", MockJsonMode.bool(true)); // Boolean instead of Integer!

        parser.parse(MockJsonMode.object(map));
    }

    // 3. 在不存在 "op" 键时却有多个键值对

    @Test(expected = ParseException.class)
    public void testMultipleKeysWithoutOpKeyThrowsParseException()
    throws ParseException {
        // Mapping has multiple key-value pairs (size > 1) and none is "op"
        // Cannot match single-argument shorthand, and lacks "op" keyword
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);

        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("first_arg", MockJsonMode.number("1"));
        map.put("second_arg", MockJsonMode.number("2"));
        map.put("third_arg", MockJsonMode.number("3"));

        parser.parse(MockJsonMode.object(map));
    }

    // 4. 其他结构边界异常

    @Test(expected = ParseException.class)
    public void testParseNullThrowsParseException() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        parser.parse(null);
    }

    @Test(expected = ParseException.class)
    public void testNonStringOpKeyThrowsParseException()
    throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        Map<String, JsonMode> map = new HashMap<String, JsonMode>();
        map.put("op", MockJsonMode.number("123")); // Not a string!
        parser.parse(MockJsonMode.object(map));
    }

    @Test(expected = ParseException.class)
    public void testNullArrayThrowsParseException() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        parser.parse(new MockJsonMode(JsonMode.Type.ARRAY, null, null, null));
    }

    @Test(expected = ParseException.class)
    public void testNullObjectThrowsParseException() throws ParseException {
        OpRegistry<Map<String, Object>> registry =
        new OpRegistry<Map<String, Object>>();
        JsonModeParser<Map<String, Object>> parser =
        new JsonModeParser<Map<String, Object>>(registry, TEST_TYPE_PROVIDER);
        parser.parse(new MockJsonMode(JsonMode.Type.OBJECT, null, null, null));
    }
}
