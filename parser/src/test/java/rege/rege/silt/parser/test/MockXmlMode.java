package rege.rege.silt.parser.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Contract;
import rege.rege.silt.xml.XmlMode;

public class MockXmlMode implements XmlMode {
    private final String tagName;
    private final String content;
    private final Map<String, String> attributes;
    private final List<XmlMode> children;

    @Contract(pure = true)
    private MockXmlMode(
        String tagName, String content, Map<String, String> attributes,
        List<XmlMode> children
    ) {
        this.tagName = tagName;
        this.content = content;
        this.attributes = attributes;
        this.children = children;
    }

    // Factory for Text node
    @Contract(value = "_ -> new", pure = true)
    public static MockXmlMode text(String content) {
        return new MockXmlMode(null, content, null, null);
    }

    // Factory for Tag node
    @Contract(value = "_, _ -> new", pure = true)
    public static MockXmlMode tag(String tagName, XmlMode... children) {
        return new MockXmlMode(
            tagName, null, null,
            children.length > 0 ? Arrays.asList(children) : null
        );
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static MockXmlMode tagWithAttrs(
        String tagName, Map<String, String> attrs, XmlMode... children
    ) {
        return new MockXmlMode(
            tagName, null, attrs,
            children.length > 0 ? Arrays.asList(children) : null
        );
    }

    public String getTagName() {
        return this.tagName;
    }

    public String getContent() {
        return this.content;
    }

    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    public List<XmlMode> getChildren() {
        return this.children;
    }
}
