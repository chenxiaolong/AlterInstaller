package com.chiller3.alterinstaller;

import android.text.TextUtils;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlterInstallerSerializer implements XmlSerializer {
    public interface OnChangeListener {
        void onFieldChanged(String packageName, String field, String oldValue, String newValue);
    }

    private static final Set<String> UPDATABLE_ATTRIBUTES;

    static {
        HashSet<String> attrs = new HashSet<>();
        attrs.add("installer");
        attrs.add("installInitiator");
        attrs.add("updateOwner");
        UPDATABLE_ATTRIBUTES = attrs;
    }

    private final XmlSerializer inner;
    private final Map<String, PackageConfig> packageToConfig;
    private final OnChangeListener listener;

    private final ArrayList<String> tags = new ArrayList<>();
    private String packageName = null;
    private PackageConfig packageConfig = null;
    private final HashSet<String> seenAttributes = new HashSet<>();

    public AlterInstallerSerializer(XmlSerializer inner, Map<String, PackageConfig> packageToConfig,
                                    OnChangeListener listener) {
        this.inner = inner;
        this.packageToConfig = packageToConfig;
        this.listener = listener;
    }

    private boolean isPackageElement() {
        return tags.size() == 2 && "packages".equals(tags.get(0)) && "package".equals(tags.get(1));
    }

    private String getPackageAttribute(String attribute) {
        return switch (attribute) {
            case "installer", "installInitiator" -> packageConfig.installer();
            case "updateOwner" -> packageConfig.updateOwner();
            default -> null;
        };
    }

    private void writeMissingAttributes(String namespace) throws IOException {
        if (packageConfig != null && isPackageElement()) {
            for (String attribute : UPDATABLE_ATTRIBUTES) {
                if (!seenAttributes.contains(attribute)) {
                    XmlSerializer result = attribute(namespace, attribute, null);
                    assert result == this;
                }
            }
        }
    }

    @Override
    public void setFeature(String name, boolean state) throws IllegalArgumentException, IllegalStateException {
        inner.setFeature(name, state);
    }

    @Override
    public boolean getFeature(String name) {
        return inner.getFeature(name);
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException, IllegalStateException {
        inner.setProperty(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return inner.getProperty(name);
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.setOutput(os, encoding);
    }

    @Override
    public void setOutput(Writer writer) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.setOutput(writer);
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.startDocument(encoding, standalone);
    }

    @Override
    public void endDocument() throws IOException, IllegalArgumentException, IllegalStateException {
        inner.endDocument();
    }

    @Override
    public void setPrefix(String prefix, String namespace) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.setPrefix(prefix, namespace);
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) throws IllegalArgumentException {
        return inner.getPrefix(namespace, generatePrefix);
    }

    @Override
    public int getDepth() {
        return inner.getDepth();
    }

    @Override
    public String getNamespace() {
        return inner.getNamespace();
    }

    @Override
    public String getName() {
        return inner.getName();
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException, IllegalArgumentException, IllegalStateException {
        writeMissingAttributes(namespace);

        XmlSerializer result = inner.startTag(namespace, name);
        assert result == inner;

        tags.add(name);

        return this;
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException, IllegalArgumentException, IllegalStateException {
        if (isPackageElement()) {
            if ("name".equals(name)) {
                // PackageManager always serializes the name first, so we just rely on the ordering
                // when updating the other fields.
                packageName = value;
                packageConfig = packageToConfig.get(value);
            } else if (packageConfig != null && UPDATABLE_ATTRIBUTES.contains(name)) {
                seenAttributes.add(name);

                String newValue = getPackageAttribute(name);
                if (newValue != null) {
                    if (listener != null) {
                        listener.onFieldChanged(packageName, name, value, newValue);
                    }

                    value = newValue;
                }
            }
        }

        if (value != null) {
            XmlSerializer result = inner.attribute(namespace, name, value);
            assert result == inner;
        }

        return this;
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException, IllegalArgumentException, IllegalStateException {
        writeMissingAttributes(namespace);

        if (isPackageElement()) {
            packageName = null;
            packageConfig = null;
            seenAttributes.clear();
        }

        XmlSerializer result = inner.endTag(namespace, name);
        assert result == inner;

        if (tags.isEmpty()) {
            throw new IllegalStateException("Tag stack is empty");
        }

        String prev = tags.remove(tags.size() - 1);
        if (!TextUtils.equals(prev, name)) {
            throw new IllegalStateException("Expected to pop " + name + ", but have " + prev);
        }

        return this;
    }

    @Override
    public XmlSerializer text(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        XmlSerializer result = inner.text(text);
        assert result == inner;

        return this;
    }

    @Override
    public XmlSerializer text(char[] buf, int start, int len) throws IOException, IllegalArgumentException, IllegalStateException {
        XmlSerializer result = inner.text(buf, start, len);
        assert result == inner;

        return this;
    }

    @Override
    public void cdsect(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.cdsect(text);
    }

    @Override
    public void entityRef(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.entityRef(text);
    }

    @Override
    public void processingInstruction(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.processingInstruction(text);
    }

    @Override
    public void comment(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.comment(text);
    }

    @Override
    public void docdecl(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.docdecl(text);
    }

    @Override
    public void ignorableWhitespace(String text) throws IOException, IllegalArgumentException, IllegalStateException {
        inner.ignorableWhitespace(text);
    }

    @Override
    public void flush() throws IOException {
        inner.flush();
    }
}
