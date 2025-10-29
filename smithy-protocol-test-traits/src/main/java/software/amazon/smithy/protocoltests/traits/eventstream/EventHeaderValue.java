/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;

public abstract class EventHeaderValue<T> implements ToNode {
    private static final Logger LOGGER = Logger.getLogger(EventHeaderValue.class.getName());

    protected final T value;
    private final Type type;

    private EventHeaderValue(Type type, T value) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        BOOLEAN,
        BYTE,
        SHORT,
        INTEGER,
        LONG,
        BLOB,
        STRING,
        TIMESTAMP;
    }

    public boolean asBoolean() {
        throw new UnsupportedOperationException("Member boolean not supported for union of type: " + type);
    }

    public byte asByte() {
        throw new UnsupportedOperationException("Member byte not supported for union of type: " + type);
    }

    public short asShort() {
        throw new UnsupportedOperationException("Member short not supported for union of type: " + type);
    }

    public int asInteger() {
        throw new UnsupportedOperationException("Member int not supported for union of type: " + type);
    }

    public long asLong() {
        throw new UnsupportedOperationException("Member long not supported for union of type: " + type);
    }

    public byte[] asBlob() {
        throw new UnsupportedOperationException("Member blob not supported for union of type: " + type);
    }

    public String asString() {
        throw new UnsupportedOperationException("Member string not supported for union of type: " + type);
    }

    public Instant asTimestamp() {
        throw new UnsupportedOperationException("Member timestamp not supported for union of type: " + type);
    }

    public T getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, getValue());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Objects.equals(getValue(), ((EventHeaderValue<?>) other).getValue());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EventHeaderValue<?> fromNode(Node node) {
        ObjectNode objectNode = node.expectObjectNode();
        Builder builder = builder();

        for (Map.Entry<StringNode, Node> pair : objectNode.getMembers().entrySet()) {
            Node value = pair.getValue();
            Type headerType = Type.valueOf(pair.getKey().getValue().toUpperCase(Locale.ENGLISH));
            switch (headerType) {
                case BOOLEAN:
                    builder.setBoolean(value.expectBooleanNode().getValue());
                    break;
                case BYTE:
                    builder.setByte(value.expectNumberNode().getValue().byteValue());
                    break;
                case SHORT:
                    builder.setShort(value.expectNumberNode().getValue().shortValue());
                    break;
                case INTEGER:
                    builder.setInteger(value.expectNumberNode().getValue().intValue());
                    break;
                case LONG:
                    builder.setLong(value.expectNumberNode().getValue().longValue());
                    break;
                case BLOB:
                    builder.setBlob(Base64.getDecoder().decode(value.expectStringNode().getValue()));
                    break;
                case STRING:
                    builder.setString(value.expectStringNode().getValue());
                    break;
                case TIMESTAMP:
                    if (value.isNumberNode()) {
                        builder.setTimestamp(value.expectNumberNode().getValue().longValue());
                    } else {
                        builder.setTimestamp(value.expectStringNode().getValue());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected header value type: " + headerType);
            }
        }

        return builder.build();
    }

    public static final class BooleanMember extends EventHeaderValue<Boolean> {
        public BooleanMember(boolean value) {
            super(Type.BOOLEAN, value);
        }

        @Override
        public boolean asBoolean() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(value))
                    .build();
        }
    }

    public static final class ByteMember extends EventHeaderValue<Byte> {
        public ByteMember(byte value) {
            super(Type.BYTE, value);
        }

        @Override
        public byte asByte() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(value))
                    .build();
        }
    }

    public static final class ShortMember extends EventHeaderValue<Short> {
        public ShortMember(short value) {
            super(Type.SHORT, value);
        }

        @Override
        public short asShort() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(value))
                    .build();
        }
    }

    public static final class IntegerMember extends EventHeaderValue<Integer> {
        public IntegerMember(int value) {
            super(Type.INTEGER, value);
        }

        @Override
        public int asInteger() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(value))
                    .build();
        }
    }

    public static final class LongMember extends EventHeaderValue<Long> {
        public LongMember(long value) {
            super(Type.LONG, value);
        }

        @Override
        public long asLong() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(value))
                    .build();
        }
    }

    public static final class BlobMember extends EventHeaderValue<byte[]> {
        public BlobMember(byte[] value) {
            super(Type.BLOB, value);
        }

        public BlobMember(String value) {
            super(Type.BLOB, value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public byte[] asBlob() {
            return value;
        }

        @Override
        public String asString() {
            return Base64.getEncoder().encodeToString(value);
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(asString()))
                    .build();
        }
    }

    public static final class StringMember extends EventHeaderValue<String> {
        public StringMember(String value) {
            super(Type.STRING, value);
        }

        @Override
        public String asString() {
            return value;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(value))
                    .build();
        }
    }

    public static final class TimestampMember extends EventHeaderValue<Instant> {
        public TimestampMember(Instant value) {
            super(Type.TIMESTAMP, value);
        }

        public TimestampMember(String value) {
            super(Type.TIMESTAMP, Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value)));
        }

        public TimestampMember(long value) {
            super(Type.TIMESTAMP, Instant.ofEpochSecond(value));
        }

        @Override
        public Instant asTimestamp() {
            return value;
        }

        @Override
        public String asString() {
            return DateTimeFormatter.ISO_INSTANT.format(value);
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                    .withMember(getType().name().toLowerCase(Locale.ENGLISH), Node.from(asString()))
                    .build();
        }
    }

    public static final class Builder implements SmithyBuilder<EventHeaderValue<?>> {
        private EventHeaderValue<?> value;

        @Override
        public EventHeaderValue<?> build() {
            return Objects.requireNonNull(value);
        }

        public Builder setBoolean(boolean value) {
            return setValue(new BooleanMember(value));
        }

        public Builder setByte(byte value) {
            return setValue(new ByteMember(value));
        }

        public Builder setShort(short value) {
            return setValue(new ShortMember(value));
        }

        public Builder setInteger(int value) {
            return setValue(new IntegerMember(value));
        }

        public Builder setLong(long value) {
            return setValue(new LongMember(value));
        }

        public Builder setBlob(byte[] value) {
            return setValue(new BlobMember(value));
        }

        public Builder setBlob(String value) {
            return setValue(new BlobMember(value));
        }

        public Builder setString(String value) {
            return setValue(new StringMember(value));
        }

        public Builder setTimestamp(Instant value) {
            return setValue(new TimestampMember(value));
        }

        public Builder setTimestamp(String value) {
            return setValue(new TimestampMember(value));
        }

        public Builder setTimestamp(long value) {
            return setValue(new TimestampMember(value));
        }

        private Builder setValue(EventHeaderValue<?> value) {
            if (this.value != null) {
                throw new IllegalArgumentException("Only one value may be set for unions.");
            }
            this.value = value;
            return this;
        }
    }
}
