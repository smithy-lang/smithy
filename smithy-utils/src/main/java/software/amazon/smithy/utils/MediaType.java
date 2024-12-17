/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implements a simple media type parser based on the Content-Type grammar defined in
 * <a href="https://tools.ietf.org/html/rfc9110#section-8.3">RFC 9110</a>.
 *
 * <p>The type, subtype, and parameter names are all canonicalized to
 * lowercase strings.
 */
public final class MediaType {

    private final String value;
    private final String type;
    private final String subtype;
    private final Map<String, String> parameters;

    private MediaType(String value, String type, String subtype, Map<String, String> parameters) {
        this.value = value;
        this.type = type;
        this.subtype = subtype;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    /**
     * Create a parsed MediaType from the given string.
     *
     * @param value Media type to parse (e.g., application/json).
     * @return Returns the parsed media type.
     * @throws RuntimeException if the media type is invalid.
     */
    public static MediaType from(String value) {
        Parser parser = new Parser(value);
        parser.parse();
        return new MediaType(parser.input().toString(), parser.type, parser.subtype, parser.parameters);
    }

    /**
     * Detects if the given media type string is JSON, meaning it
     * is "application/json" or uses the "+json" structured syntax
     * suffix.
     *
     * @param mediaType Media type to parse and test if it's JSON.
     * @return Returns true if the given media type is JSON.
     */
    public static boolean isJson(String mediaType) {
        MediaType type = from(mediaType);
        return (type.getType().equals("application") && type.getSubtypeWithoutSuffix().equals("json"))
                || type.getSuffix().filter(s -> s.equals("json")).isPresent();
    }

    /**
     * Gets the "type" of the media type.
     *
     * @return Returns the type (e.g., "application").
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the "subtype" of the media type.
     *
     * @return Returns the subtype (e.g., "json", "foo+json").
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * Gets the "subtype" of the media type with no structured syntax suffix.
     *
     * <p>For example given, "application/foo+json", this method returns
     * "foo". Given "application/foo+baz+json", this method returns
     * "foo+baz".
     *
     * @return Returns the subtype (e.g., "json", "foo+json").
     */
    public String getSubtypeWithoutSuffix() {
        int position = subtype.lastIndexOf('+');
        return position == -1 ? getSubtype() : subtype.substring(0, position);
    }

    /**
     * Gets the immutable map of parameters.
     *
     * @return Returns the parameters.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Gets the optional structured syntax suffix.
     *
     * <p>For example given, "application/foo+json", this method returns
     * "json". Given "application/foo+baz+json", this method returns
     * "json". Given "application/json", this method returns an empty
     * {@code Optional}.
     *
     * @return Returns the optional structured syntax suffix value with no "+".
     */
    public Optional<String> getSuffix() {
        int position = subtype.lastIndexOf('+');
        return position == -1 || position == subtype.length() - 1
                ? Optional.empty()
                : Optional.of(subtype.substring(position + 1));
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof MediaType)) {
            return false;
        }
        MediaType mediaType = (MediaType) o;
        return mediaType.type.equals(type)
                && mediaType.subtype.equals(subtype)
                && mediaType.parameters.equals(parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subtype, parameters);
    }

    private static final class Parser extends SimpleParser {
        // See https://tools.ietf.org/html/rfc9110#section-5.6.2
        // token          = 1*tchar
        // tchar          = "!" / "#" / "$" / "%" / "&" / "'" / "*"
        //                / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
        //                / DIGIT / ALPHA
        //                ; any VCHAR, except delimiters
        private static final Set<Character> TOKEN = new LinkedHashSet<>();

        static {
            TOKEN.add('!');
            TOKEN.add('#');
            TOKEN.add('$');
            TOKEN.add('%');
            TOKEN.add('&');
            TOKEN.add('\'');
            TOKEN.add('*');
            TOKEN.add('+');
            TOKEN.add('-');
            TOKEN.add('.');
            TOKEN.add('^');
            TOKEN.add('_');
            TOKEN.add('`');
            TOKEN.add('|');
            TOKEN.add('~');
            for (char c = '0'; c <= '9'; c++) {
                TOKEN.add(c);
            }
            for (char c = 'a'; c <= 'z'; c++) {
                TOKEN.add(c);
            }
            for (char c = 'A'; c <= 'Z'; c++) {
                TOKEN.add(c);
            }
        }

        private String type;
        private String subtype;
        private final Map<String, String> parameters = new LinkedHashMap<>();

        Parser(String value) {
            super(value);
        }

        private void parse() {
            // From: https://tools.ietf.org/html/rfc9110#section-8.3.1
            // The type, subtype, and parameter name tokens are case-insensitive.
            //     media-type = type "/" subtype *( OWS ";" OWS parameter )
            //     type       = token.
            type = parseToken().toLowerCase(Locale.US);
            expect('/');

            //     subtype    = token
            subtype = parseToken().toLowerCase(Locale.US);
            ws();

            //     parameter      = token "=" ( token / quoted-string )
            while (!eof()) {
                expect(';');
                ws();
                String name = parseToken().toLowerCase(Locale.US);
                expect('=');
                String value = peek() == '"'
                        ? parseQuotedString()
                        : parseToken();
                parameters.put(name, value);
                ws();
            }
        }

        private String parseToken() {
            int start = position();
            consumeWhile(c -> TOKEN.contains((char) c));

            // Fail if the token was empty.
            if (start == position()) {
                char[] chars = new char[TOKEN.size()];
                int i = 0;
                for (Character character : TOKEN) {
                    chars[i++] = character;
                }
                expect(chars);
            }

            return sliceFrom(start);
        }

        // See https://tools.ietf.org/html/rfc9110#section-5.6.4
        // quoted-string  = DQUOTE *( qdtext / quoted-pair ) DQUOTE
        // qdtext         = HTAB / SP /%x21 / %x23-5B / %x5D-7E / obs-text
        // obs-text       = %x80-FF
        // quoted-pair    = "\" ( HTAB / SP / VCHAR / obs-text )
        private String parseQuotedString() {
            StringBuilder result = new StringBuilder();
            expect('"');

            while (!eof()) {
                char next = peek();
                if (next == '"') {
                    break;
                }
                skip();

                // The backslash octet ("\") can be used as a single-octet quoting
                // mechanism within quoted-string and comment constructs.  Recipients
                // that process the value of a quoted-string MUST handle a quoted-pair
                // as if it were replaced by the octet following the backslash.
                if (next == '\\') {
                    if (eof()) {
                        throw syntax("Expected character after escape");
                    }
                    result.append(peek());
                    skip();
                } else {
                    result.append(next);
                }
            }

            expect('"');
            return result.toString();
        }
    }
}
