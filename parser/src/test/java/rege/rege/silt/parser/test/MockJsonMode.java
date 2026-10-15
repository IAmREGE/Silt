package rege.rege.silt.parser.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import rege.rege.silt.json.JsonMode;

public class MockJsonMode implements JsonMode {
    private final Type type;
    private final String primitive;
    private final List<JsonMode> array;
    private final Map<String, JsonMode> object;

    @Contract(pure = true)
    public MockJsonMode(
        Type type, String primitive, List<JsonMode> array,
        Map<String, JsonMode> object
    ) {
        this.type = type;
        this.primitive = primitive;
        this.array = array;
        this.object = object;
    }

    @Contract(value = "_ -> new", pure = true)
    public static MockJsonMode string(String val) {
        return new MockJsonMode(Type.STRING, val, null, null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static MockJsonMode number(String raw) {
        return new MockJsonMode(Type.NUMBER, raw, null, null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static MockJsonMode bool(boolean val) {
        return new MockJsonMode(Type.BOOLEAN, String.valueOf(val), null, null);
    }

    @Contract(value = "-> new", pure = true)
    public static MockJsonMode nullVal() {
        return new MockJsonMode(Type.NULL, null, null, null);
    }

    @Contract(value = "_ -> new", pure = true)
    public static MockJsonMode array(JsonMode[] elements) {
        return new MockJsonMode(
            Type.ARRAY, null, Arrays.asList(elements), null
        );
    }

    @Contract(value = "_ -> new", pure = true)
    public static MockJsonMode object(Map<String, JsonMode> map) {
        return new MockJsonMode(Type.OBJECT, null, null, map);
    }

    @NotNull
    public Type getType() {
        return this.type;
    }

    public String getPrimitive() {
        return this.primitive;
    }

    public List<JsonMode> getArray() {
        return this.array;
    }

    public Map<String, JsonMode> getObject() {
        return this.object;
    }
}
