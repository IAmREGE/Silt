package rege.rege.silt.xml;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public interface XmlMode {
    @Nullable
    String getTagName();

    @Nullable
    Map<String, String> getAttributes();

    @Nullable
    String getContent();

    @Nullable
    List<XmlMode> getChildren();
}
