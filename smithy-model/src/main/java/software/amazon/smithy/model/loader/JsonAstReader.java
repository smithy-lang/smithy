/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.NumberUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * An {@link AstReader} backed by a self-contained, stateful JSON lexer used when loading JSON model
 * files. All JSON parsing machinery is private; callers see only the {@link AstReader} cursor plus
 * {@link #parse} for materializing a whole document as a {@link Node}.
 *
 * <p>The lexer is a Jackson-style pull tokenizer (no per-token allocation; the current token's value
 * and location live in fields) that validates as it reads and throws {@link ModelSyntaxException} on
 * malformed input. Low-level routines are adapted from the minimal-json parser the loader previously
 * used, preserving behavior and error messages.
 */
@SmithyInternalApi
public final class JsonAstReader implements AstReader {

    // Internal token kinds. Not exposed: the AstReader surface speaks in AstReader.Type.
    private enum Token {
        START_OBJECT,
        END_OBJECT,
        START_ARRAY,
        END_ARRAY,
        KEY,
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }

    private static final int MAX_NESTING_LEVEL = 1000;
    private static final int MIN_BUFFER_SIZE = 10;
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private static final int CTX_ROOT = 0;
    private static final int CTX_ARRAY = 1;
    private static final int CTX_OBJECT = 2;

    // Short-string dedup cache (see endCaptureDeduped). 8 single-byte chars pack into a long key.
    private static final int MAX_DEDUP_CHARS = 8;
    private static final int DEDUP_SIZE = 512; // power of two
    private static final int DEDUP_MASK = DEDUP_SIZE - 1;

    private static final class StringDedupCache {
        final long[] keys = new long[DEDUP_SIZE];
        final String[] vals = new String[DEDUP_SIZE];
    }

    // One cache per thread, reused across parses without clearing (the packed key is exact, so a
    // stale entry is byte-identical to any new match).
    private static final ThreadLocal<StringDedupCache> DEDUP_CACHE = ThreadLocal.withInitial(StringDedupCache::new);

    private final String filename;
    private final boolean allowComments;
    private final Reader reader;

    private final char[] buffer;
    private int bufferOffset;
    private int index;
    private int fill;
    private int line;
    private int lineOffset;
    private int current;
    private StringBuilder captureBuffer;
    private int captureStart;

    private int[] contexts = new int[16];
    private int depth;
    private boolean primed;

    private Token currentToken;
    private String currentString;
    private String currentNumber;
    private boolean currentBoolean;
    private int tokenLine;
    private int tokenColumn;

    private SourceLocation lastKeyLocation;
    // Containers entered via startObject/startArray, so depth() matches across AstReader impls
    // regardless of when each one's cursor counts a container.
    private int enteredDepth;

    // Dedup cache arrays, fetched from the per-thread pool on the first short string.
    private long[] dedupKeys;
    private String[] dedupVals;

    private JsonAstReader(String filename, Reader reader, boolean allowComments, int bufferSize) {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        this.filename = filename;
        this.reader = reader;
        this.allowComments = allowComments;
        this.buffer = new char[bufferSize];
        this.line = 1;
        this.captureStart = -1;
        this.contexts[depth++] = CTX_ROOT;
    }

    // ===== Public entry points =====

    /**
     * Creates a reader over JSON text, positioned on the document's root value.
     */
    static JsonAstReader from(String filename, String content, boolean allowComments) {
        if (content == null) {
            throw new NullPointerException("string is null");
        }
        int bufferSize = Math.max(MIN_BUFFER_SIZE, Math.min(DEFAULT_BUFFER_SIZE, content.length()));
        JsonAstReader r = new JsonAstReader(filename, new StringReader(content), allowComments, bufferSize);
        r.advance();
        return r;
    }

    /**
     * Creates a reader over a JSON character stream, positioned on the document's root value.
     */
    static JsonAstReader from(String filename, Reader reader, boolean allowComments) {
        JsonAstReader r = new JsonAstReader(filename, reader, allowComments, DEFAULT_BUFFER_SIZE);
        r.advance();
        return r;
    }

    /**
     * Parses a complete JSON document from text into a {@link Node}.
     */
    public static Node parse(String filename, String content, boolean allowComments) {
        return parseDocument(from(filename, content, allowComments));
    }

    /**
     * Parses a complete JSON document from a reader into a {@link Node}.
     */
    public static Node parse(String filename, Reader reader, boolean allowComments) {
        return parseDocument(from(filename, reader, allowComments));
    }

    private static Node parseDocument(JsonAstReader r) {
        Node value = r.readValueAsNode();
        r.advance(); // enforce that only trailing whitespace / EOF remains
        return value;
    }

    // ===== AstReader cursor =====

    @Override
    public Type currentType() {
        Type type = toType(currentToken);
        if (type == null) {
            throw new ModelSyntaxException("Error parsing JSON: Unexpected end of input", getSourceLocation());
        }
        return type;
    }

    @Override
    public SourceLocation currentLocation() {
        return new SourceLocation(filename, tokenLine, tokenColumn);
    }

    @Override
    public void startObject() {
        // The cursor is already on START_OBJECT; just record the nesting.
        enteredDepth++;
    }

    @Override
    public String nextKey() {
        if (advance() == Token.END_OBJECT) {
            enteredDepth--;
            return null;
        }
        String key = currentString;
        lastKeyLocation = new SourceLocation(filename, tokenLine, tokenColumn);
        advance(); // move to the member value
        return key;
    }

    @Override
    public SourceLocation lastKeyLocation() {
        return lastKeyLocation;
    }

    @Override
    public void startArray() {
        enteredDepth++;
    }

    @Override
    public boolean nextElement() {
        if (advance() == Token.END_ARRAY) {
            enteredDepth--;
            return false;
        }
        return true;
    }

    @Override
    public String expectStringValue(String label) {
        if (currentToken != Token.STRING) {
            throw new SourceException("Expected " + label + " to be a string, but found "
                    + AstReader.describe(toType(currentToken)), getSourceLocation());
        }
        return currentString;
    }

    @Override
    public void skipValue() {
        if (currentToken == Token.START_OBJECT || currentToken == Token.START_ARRAY) {
            int targetDepth = depth - 1;
            while (depth > targetDepth) {
                advance();
            }
        }
    }

    @Override
    public int depth() {
        return enteredDepth;
    }

    @Override
    public void skipToDepth(int targetDepth) {
        // Advance the lexer's own context stack down by however many entered containers we unwind.
        int lexerTarget = depth - (enteredDepth - targetDepth);
        while (depth > lexerTarget) {
            advance();
        }
        enteredDepth = targetDepth;
    }

    // ===== Node materialization (recursion over the cursor) =====

    @Override
    public Node readValueAsNode() {
        Token token = currentToken;
        if (token == null) {
            throw new ModelSyntaxException("Error parsing JSON: Unexpected end of input", getSourceLocation());
        }
        switch (token) {
            case START_OBJECT:
                return readObjectNode();
            case START_ARRAY:
                return readArrayNode();
            case STRING:
                return new StringNode(currentString, currentLocation());
            case NUMBER:
                return numberNode(currentNumber, currentLocation());
            case BOOLEAN:
                return new BooleanNode(currentBoolean, currentLocation());
            case NULL:
                return new NullNode(currentLocation());
            default:
                throw new ModelSyntaxException("Error parsing JSON: Expected value", getSourceLocation());
        }
    }

    private Node readObjectNode() {
        ObjectNode.Builder builder = ObjectNode.builder().sourceLocation(currentLocation());
        while (advance() != Token.END_OBJECT) {
            String name = currentString;
            SourceLocation keyLocation = new SourceLocation(filename, tokenLine, tokenColumn);
            advance();
            builder.withMember(new StringNode(name, keyLocation), readValueAsNode());
        }
        return builder.build();
    }

    private Node readArrayNode() {
        ArrayNode.Builder builder = ArrayNode.builder().sourceLocation(currentLocation());
        while (advance() != Token.END_ARRAY) {
            builder.withValue(readValueAsNode());
        }
        return builder.build();
    }

    private static Node numberNode(String lexeme, SourceLocation location) {
        try {
            return new NumberNode(NumberUtils.parseNumber(lexeme), location);
        } catch (NumberFormatException e) {
            // Some grammar-valid literals (e.g. huge exponents) overflow BigDecimal; surface them as
            // the parser's declared failure mode instead of a raw NumberFormatException.
            throw new ModelSyntaxException("Invalid number " + lexeme + ": " + e.getMessage(), location);
        }
    }

    private static Type toType(Token token) {
        if (token == null) {
            return null;
        }
        switch (token) {
            case START_OBJECT:
                return Type.OBJECT;
            case START_ARRAY:
                return Type.ARRAY;
            case STRING:
                return Type.STRING;
            case NUMBER:
                return Type.NUMBER;
            case BOOLEAN:
                return Type.BOOLEAN;
            case NULL:
                return Type.NULL;
            default:
                return null;
        }
    }

    // ===== Lexer =====

    private Token advance() {
        try {
            return doNext();
        } catch (IOException e) {
            throw new ModelSyntaxException("Error parsing JSON: " + e.getMessage(), getSourceLocation());
        }
    }

    private Token doNext() throws IOException {
        if (!primed) {
            primed = true;
            read();
        }
        skipWhiteSpace();

        switch (contexts[depth - 1]) {
            case CTX_ROOT:
                if (currentToken == null) {
                    if (isEndOfText()) {
                        throw expected("value");
                    }
                    return readValue();
                }
                if (!isEndOfText()) {
                    throw error("Unexpected character");
                }
                return currentToken = null;
            case CTX_ARRAY:
                return nextInArray();
            case CTX_OBJECT:
                return nextInObject();
            default:
                throw new IllegalStateException("Unknown JSON context");
        }
    }

    private Token nextInArray() throws IOException {
        if (currentToken == Token.START_ARRAY) {
            if (readChar(']')) {
                return endContainer(Token.END_ARRAY);
            }
            return readValue();
        }
        if (readChar(']')) {
            return endContainer(Token.END_ARRAY);
        }
        if (!readChar(',')) {
            throw expected("',' or ']'");
        }
        skipWhiteSpace();
        return readValue();
    }

    private Token nextInObject() throws IOException {
        if (currentToken == Token.KEY) {
            skipWhiteSpace();
            if (!readChar(':')) {
                throw expected("':'");
            }
            skipWhiteSpace();
            return readValue();
        }
        if (currentToken == Token.START_OBJECT) {
            if (readChar('}')) {
                return endContainer(Token.END_OBJECT);
            }
            return readKey();
        }
        if (readChar('}')) {
            return endContainer(Token.END_OBJECT);
        }
        if (!readChar(',')) {
            throw expected("',' or '}'");
        }
        skipWhiteSpace();
        return readKey();
    }

    private Token endContainer(Token token) {
        depth--;
        return currentToken = token;
    }

    private Token readKey() throws IOException {
        captureTokenLocation();
        if (current != '"') {
            throw expected("name");
        }
        // Object keys are the most heavily repeated strings in an AST; dedup them.
        currentString = readStringInternal(true);
        return currentToken = Token.KEY;
    }

    private Token readValue() throws IOException {
        captureTokenLocation();
        switch (current) {
            case 'n':
                return readNull();
            case 't':
                return readTrue();
            case 'f':
                return readFalse();
            case '"':
                // String values (shape targets, type names, enum values, ...) also repeat; dedup.
                currentString = readStringInternal(true);
                return currentToken = Token.STRING;
            case '[':
                pushContext(CTX_ARRAY);
                read();
                return currentToken = Token.START_ARRAY;
            case '{':
                pushContext(CTX_OBJECT);
                read();
                return currentToken = Token.START_OBJECT;
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                currentNumber = readNumberInternal();
                return currentToken = Token.NUMBER;
            default:
                throw expected("value");
        }
    }

    private void pushContext(int context) {
        if (depth == MAX_NESTING_LEVEL) {
            throw error("Nesting too deep");
        }
        if (depth == contexts.length) {
            int[] grown = new int[contexts.length * 2];
            System.arraycopy(contexts, 0, grown, 0, contexts.length);
            contexts = grown;
        }
        contexts[depth++] = context;
    }

    private Token readNull() throws IOException {
        read();
        readRequiredChar('u');
        readRequiredChar('l');
        readRequiredChar('l');
        return currentToken = Token.NULL;
    }

    private Token readTrue() throws IOException {
        read();
        readRequiredChar('r');
        readRequiredChar('u');
        readRequiredChar('e');
        currentBoolean = true;
        return currentToken = Token.BOOLEAN;
    }

    private Token readFalse() throws IOException {
        read();
        readRequiredChar('a');
        readRequiredChar('l');
        readRequiredChar('s');
        readRequiredChar('e');
        currentBoolean = false;
        return currentToken = Token.BOOLEAN;
    }

    private void readRequiredChar(char ch) throws IOException {
        if (!readChar(ch)) {
            throw expected("'" + ch + "'");
        }
    }

    private String readStringInternal(boolean dedup) throws IOException {
        read();
        startCapture();
        while (current != '"') {
            if (current == '\\') {
                pauseCapture();
                readEscape();
                startCapture();
            } else if (current < 0x20) {
                throw expected("valid string character");
            } else {
                read();
            }
        }
        String string = dedup ? endCaptureDeduped() : endCapture();
        read();
        return string;
    }

    private void readEscape() throws IOException {
        read();
        switch (current) {
            case '"':
            case '/':
            case '\\':
                captureBuffer.append((char) current);
                break;
            case 'b':
                captureBuffer.append('\b');
                break;
            case 'f':
                captureBuffer.append('\f');
                break;
            case 'n':
                captureBuffer.append('\n');
                break;
            case 'r':
                captureBuffer.append('\r');
                break;
            case 't':
                captureBuffer.append('\t');
                break;
            case 'u':
                char[] hexChars = new char[4];
                for (int i = 0; i < 4; i++) {
                    read();
                    if (!isHexDigit()) {
                        throw expected("hexadecimal digit");
                    }
                    hexChars[i] = (char) current;
                }
                captureBuffer.append((char) Integer.parseInt(new String(hexChars), 16));
                break;
            default:
                throw expected("valid escape sequence");
        }
        read();
    }

    private String readNumberInternal() throws IOException {
        startCapture();
        readChar('-');
        int firstDigit = current;
        if (!readDigit()) {
            throw expected("digit");
        }
        if (firstDigit != '0') {
            while (readDigit()) {}
        }
        readFraction();
        readExponent();
        return endCapture();
    }

    private void readFraction() throws IOException {
        if (!readChar('.')) {
            return;
        } else if (!readDigit()) {
            throw expected("digit");
        }
        while (readDigit()) {}
    }

    private void readExponent() throws IOException {
        if (!readChar('e') && !readChar('E')) {
            return;
        }
        if (!readChar('+')) {
            readChar('-');
        }
        if (!readDigit()) {
            throw expected("digit");
        }
        while (readDigit()) {}
    }

    private boolean readChar(char ch) throws IOException {
        if (current != ch) {
            return false;
        }
        read();
        return true;
    }

    private boolean readDigit() throws IOException {
        if (!isDigit()) {
            return false;
        }
        read();
        return true;
    }

    private void skipWhiteSpace() throws IOException {
        do {
            while (isWhiteSpace()) {
                read();
            }
        } while (skipComment());
    }

    private boolean skipComment() throws IOException {
        if (allowComments && current == '/') {
            read();
            if (!readChar('/')) {
                throw expected("Expected '/' to form a valid comment");
            }
            while (!isEndOfText() && current != '\n') {
                read();
            }
            if (current == '\n') {
                read();
            }
            return true;
        }
        return false;
    }

    private void read() throws IOException {
        if (index == fill) {
            if (captureStart != -1) {
                captureBuffer.append(buffer, captureStart, fill - captureStart);
                captureStart = 0;
            }
            bufferOffset += fill;
            fill = reader.read(buffer, 0, buffer.length);
            index = 0;
            if (fill == -1) {
                current = -1;
                index++;
                return;
            }
        }
        if (current == '\n') {
            line++;
            lineOffset = bufferOffset + index;
        }
        current = buffer[index++];
    }

    private void startCapture() {
        if (captureBuffer == null) {
            captureBuffer = new StringBuilder();
        }
        captureStart = index - 1;
    }

    private void pauseCapture() {
        int end = current == -1 ? index : index - 1;
        captureBuffer.append(buffer, captureStart, end - captureStart);
        captureStart = -1;
    }

    private String endCapture() {
        int start = captureStart;
        int end = index - 1;
        captureStart = -1;
        if (captureBuffer.length() > 0) {
            captureBuffer.append(buffer, start, end - start);
            String captured = captureBuffer.toString();
            captureBuffer.setLength(0);
            return captured;
        }
        return new String(buffer, start, end - start);
    }

    /**
     * Like {@link #endCapture()}, but deduplicates short unescaped strings so repeated keys/values
     * reuse one {@link String}. The chars of a short string pack into a {@code long} that serves as an
     * exact identity key (content chars are {@code >= 0x20} and packing requires each {@code < 0x100},
     * so length is implicit and keys never collide on distinct strings), making a cache hit a single
     * {@code long} compare. The cache overwrites on collision and is pooled per thread. Anything that
     * doesn't fit falls back to {@link #endCapture()}.
     */
    private String endCaptureDeduped() {
        if (captureBuffer != null && captureBuffer.length() > 0) {
            return endCapture(); // escaped or split across a buffer refill
        }

        int start = captureStart;
        int len = index - 1 - start;
        if (len == 0) {
            captureStart = -1;
            return "";
        }
        if (len > MAX_DEDUP_CHARS) {
            return endCapture();
        }

        long key = 0;
        for (int i = 0; i < len; i++) {
            char c = buffer[start + i];
            if (c >= 0x100) {
                return endCapture(); // multi-byte char: packing wouldn't be an exact identity
            }
            key = (key << 8) | c;
        }
        captureStart = -1;

        long[] keys = dedupKeys;
        String[] vals = dedupVals;
        if (keys == null) {
            StringDedupCache cache = DEDUP_CACHE.get();
            keys = dedupKeys = cache.keys;
            vals = dedupVals = cache.vals;
        }

        // Fibonacci hash so keys differing only in low bits spread across slots.
        int slot = (int) ((key * 0x9E3779B97F4A7C15L) >>> 48) & DEDUP_MASK;
        if (keys[slot] == key) {
            return vals[slot];
        }

        String s = new String(buffer, start, len);
        keys[slot] = key;
        vals[slot] = s;
        return s;
    }

    private void captureTokenLocation() {
        tokenLine = line;
        tokenColumn = bufferOffset + index - 1 - lineOffset + 1;
    }

    private SourceLocation getSourceLocation() {
        int offset = bufferOffset + index - 1;
        int column = offset - lineOffset + 1;
        return new SourceLocation(filename, line, column);
    }

    private ModelSyntaxException expected(String expected) {
        if (isEndOfText()) {
            return error("Unexpected end of input");
        }
        return error("Expected " + expected);
    }

    private ModelSyntaxException error(String message) {
        return new ModelSyntaxException("Error parsing JSON: " + message, getSourceLocation());
    }

    private boolean isWhiteSpace() {
        return current == ' ' || current == '\t' || current == '\n' || current == '\r';
    }

    private boolean isDigit() {
        return current >= '0' && current <= '9';
    }

    private boolean isHexDigit() {
        return current >= '0' && current <= '9'
                || current >= 'a' && current <= 'f'
                || current >= 'A' && current <= 'F';
    }

    private boolean isEndOfText() {
        return current == -1;
    }
}
