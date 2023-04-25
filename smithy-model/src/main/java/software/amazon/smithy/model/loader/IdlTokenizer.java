/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Iterates over a Smithy IDL model as a series of tokens.
 */
@SmithyUnstableApi
final class IdlTokenizer implements Iterator<IdlToken> {

    /** Only allow nesting up to 64 arrays/objects in node values. */
    private static final int MAX_NESTING_LEVEL = 64;

    private final SimpleParser parser;
    private final String filename;
    private final Function<CharSequence, String> stringTable;

    private IdlToken currentTokenType;
    private int currentTokenStart = -1;
    private int currentTokenEnd = -1;
    private int currentTokenLine = -1;
    private int currentTokenColumn = -1;
    private Number currentTokenNumber;
    private CharSequence currentTokenStringSlice;
    private String currentTokenError;
    private final Deque<CharSequence> docCommentLines = new ArrayDeque<>();
    private final Consumer<ValidationEvent> validationEventListener;

    private IdlTokenizer(Builder builder) {
        this.filename = builder.filename;
        this.stringTable = builder.stringTable;
        this.parser = new SimpleParser(SmithyBuilder.requiredState("model", builder.model), MAX_NESTING_LEVEL);
        this.validationEventListener = builder.validationEventListener;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements SmithyBuilder<IdlTokenizer> {
        private String filename = SourceLocation.NONE.getFilename();
        private CharSequence model;
        private Function<CharSequence, String> stringTable = CharSequence::toString;
        private Consumer<ValidationEvent> validationEventListener = event -> { };

        private Builder() { }

        /**
         * The filename to associate with each token and errors.
         *
         * <p>Defaults to the filename of {@link SourceLocation#NONE}.
         *
         * @param filename Filename where the model comes from.
         * @return Returns builder.
         */
        public Builder filename(String filename) {
            this.filename = Objects.requireNonNull(filename);
            return this;
        }

        /**
         * Sets the required model IDL content to parse.
         *
         * @param model Model IDL content.
         * @return Returns the builder.
         */
        public Builder model(CharSequence model) {
            this.model = model;
            return this;
        }

        /**
         * Allows the use of a custom string table, used to convert {@link CharSequence}s into Strings.
         *
         * <p>Uses {@link CharSequence#toString()} by default. A string table cache is used when the tokenizer is
         * used by {@link ModelAssembler}.
         *
         * @param stringTable String table to use.
         * @return Returns the builder.
         */
        public Builder stringTable(Function<CharSequence, String> stringTable) {
            this.stringTable = Objects.requireNonNull(stringTable);
            return this;
        }

        /**
         * Sets a listener to receive warnings about syntax issues in the model.
         *
         * <p>This is currently package private and only used to warn about invalid documentation comments.
         *
         * @param validationEventListener Listener that receives warnings.
         * @return Returns the builder.
         */
        Builder validationEventListener(Consumer<ValidationEvent> validationEventListener) {
            this.validationEventListener = Objects.requireNonNull(validationEventListener);
            return this;
        }

        @Override
        public IdlTokenizer build() {
            return new IdlTokenizer(this);
        }
    }

    /**
     * Get a borrowed slice of the input being parsed.
     *
     * @param start Start position to get, inclusive.
     * @param end   End position to stop at, exclusive.
     * @return Returns the slice.
     */
    CharSequence getInput(int start, int end) {
        return CharBuffer.wrap(parser.input(), start, end);
    }

    /**
     * Intern a string and cache the interned value for subsequent retrieval.
     *
     * <p>This method should only be used with strings that are frequently used, for example, object keys, shape
     * properties, namespaces, keywords (e.g., true, false, null, namespace, use, string, structure, trait IDs, etc).
     *
     * @param sequence Characters to convert to a string.
     * @return Returns the String representation of {@code sequence}.
     */
    public String internString(CharSequence sequence) {
        return stringTable.apply(sequence);
    }

    /**
     * Increase the nesting level of the tokenizer.
     */
    public void increaseNestingLevel() {
        try {
            parser.increaseNestingLevel();
        } catch (RuntimeException e) {
            throw syntax("Parser exceeded maximum allowed depth of " + MAX_NESTING_LEVEL, getCurrentTokenLocation());
        }
    }

    /**
     * Decrease the nesting level of the tokenizer.
     */
    public void decreaseNestingLevel() {
        parser.decreaseNestingLevel();
    }

    /**
     * Get the current position of the tokenizer.
     *
     * @return Returns the absolute position.
     */
    public int getPosition() {
        return parser.position();
    }

    /**
     * Get the current line number of the tokenizer, starting at 1.
     *
     * @return Get the current line number.
     */
    public int getLine() {
        return parser.line();
    }

    /**
     * Get the current column number of the tokenizer, starting at 1.
     *
     * @return Get the current column number.
     */
    public int getColumn() {
        return parser.column();
    }

    /**
     * Get the current {@link IdlToken}.
     *
     * @return Return the current token type.
     */
    public IdlToken getCurrentToken() {
        if (currentTokenType == null) {
            next();
        }
        return currentTokenType;
    }

    /**
     * Get the line of the current token.
     *
     * @return Return the line of the current token.
     */
    public int getCurrentTokenLine() {
        getCurrentToken();
        return currentTokenLine;
    }

    /**
     * Get the column of the current token.
     *
     * @return Return the column of the current token.
     */
    public int getCurrentTokenColumn() {
        getCurrentToken();
        return currentTokenColumn;
    }

    /**
     * Get the start position of the current token.
     *
     * @return Return the start position of the current token.
     */
    public int getCurrentTokenStart() {
        getCurrentToken();
        return currentTokenStart;
    }

    /**
     * Get the span, or length, of the current token.
     *
     * @return Return the current token span.
     */
    public int getCurrentTokenSpan() {
        getCurrentToken();
        return currentTokenEnd - currentTokenStart;
    }

    /**
     * Get the source location of the current token.
     *
     * @return Return the current token source location.
     */
    public SourceLocation getCurrentTokenLocation() {
        getCurrentToken();
        return new SourceLocation(filename, currentTokenLine, currentTokenColumn);
    }

    /**
     * Get the lexeme of the current token.
     *
     * @return Returns the lexeme of the current token.
     */
    public CharSequence getCurrentTokenLexeme() {
        getCurrentToken();
        return CharBuffer.wrap(parser.input(), currentTokenStart, currentTokenEnd);
    }

    /**
     * If the current token is a string or text block, get the parsed content as a CharSequence.
     * If the current token is an identifier, the lexeme of the identifier is returned.
     *
     * @return Returns the parsed string content associated with the current token.
     * @throws ModelSyntaxException if the current token is not a string, text block, or identifier.
     */
    public CharSequence getCurrentTokenStringSlice() {
        getCurrentToken();
        if (currentTokenStringSlice != null) {
            return currentTokenStringSlice;
        } else if (currentTokenType == IdlToken.IDENTIFIER) {
            return getCurrentTokenLexeme();
        } else {
            throw syntax("The current token must be string or identifier but found: "
                         + currentTokenType.getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
    }

    /**
     * If the current token is a number, get the associated parsed number.
     *
     * @return Returns the parsed number associated with the current token.
     * @throws ModelSyntaxException if the current token is not a number.
     */
    public Number getCurrentTokenNumberValue() {
        getCurrentToken();
        if (currentTokenNumber == null) {
            throw syntax("The current token must be number but found: "
                         + currentTokenType.getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
        return currentTokenNumber;
    }

    /**
     * If the current token is an error, get the error message associated with the token.
     *
     * @return Returns the associated error message.
     * @throws ModelSyntaxException if the current token is not an error.
     */
    public String getCurrentTokenError() {
        getCurrentToken();
        if (currentTokenType != IdlToken.ERROR) {
            throw syntax("The current token must be an error but found: "
                         + currentTokenType.getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
        return currentTokenError == null ? "" : currentTokenError;
    }

    @Override
    public boolean hasNext() {
        return currentTokenType != IdlToken.EOF;
    }

    @Override
    public IdlToken next() {
        currentTokenStringSlice = null;
        currentTokenNumber = null;
        currentTokenColumn = parser.column();
        currentTokenLine = parser.line();
        currentTokenStart = parser.position();
        currentTokenEnd = currentTokenStart;
        int c = parser.peek();

        switch (c) {
            case SimpleParser.EOF:
                if (currentTokenType == IdlToken.EOF) {
                    throw new NoSuchElementException("Expected another token but reached EOF");
                }
                currentTokenEnd = parser.position();
                return currentTokenType = IdlToken.EOF;
            case ' ':
            case '\t':
                return tokenizeSpace();
            case '\r':
            case '\n':
                return tokenizeNewline();
            case ',':
                return singleCharToken(IdlToken.COMMA);
            case '@':
                return singleCharToken(IdlToken.AT);
            case '$':
                return singleCharToken(IdlToken.DOLLAR);
            case '.':
                return singleCharToken(IdlToken.DOT);
            case '{':
                return singleCharToken(IdlToken.LBRACE);
            case '}':
                return singleCharToken(IdlToken.RBRACE);
            case '[':
                return singleCharToken(IdlToken.LBRACKET);
            case ']':
                return singleCharToken(IdlToken.RBRACKET);
            case '(':
                return singleCharToken(IdlToken.LPAREN);
            case ')':
                return singleCharToken(IdlToken.RPAREN);
            case '#':
                return singleCharToken(IdlToken.POUND);
            case '=':
                return singleCharToken(IdlToken.EQUAL);
            case ':':
                return parseColon();
            case '"':
                return parseString();
            case '/':
                return parseComment();
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
                return parseNumber();
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '_':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                return parseIdentifier();
            default:
                currentTokenError = "Unexpected character: '" + ((char) c) + '\'';
                return singleCharToken(IdlToken.ERROR);
        }
    }

    /**
     * Skip spaces.
     */
    public void skipSpaces() {
        getCurrentToken();
        while (currentTokenType == IdlToken.SPACE) {
            next();
        }
    }

    /**
     * Skip until the current token is not {@link IdlToken#SPACE}, {@link IdlToken#COMMA}, {@link IdlToken#COMMENT},
     * or {@link IdlToken#NEWLINE}.
     */
    public void skipWs() {
        getCurrentToken();
        while (currentTokenType.isWhitespace()) {
            next();
        }
    }

    /**
     * Skip until the current token is not {@link IdlToken#SPACE}, {@link IdlToken#COMMA}, {@link IdlToken#COMMENT},
     * or {@link IdlToken#NEWLINE}, or {@link IdlToken#DOC_COMMENT}.
     */
    public void skipWsAndDocs() {
        getCurrentToken();
        while ((currentTokenType.isWhitespace() || currentTokenType == IdlToken.DOC_COMMENT)) {
            next();
        }
    }

    /**
     * Check if the current token is an identifier, and if its lexeme starts with the given character {@code c}.
     *
     * <p>This method does not throw if the current token is not an identifier.
     *
     * @return Returns true if the token is an identifier that starts with {@code c}.
     */
    public boolean doesCurrentIdentifierStartWith(char c) {
        getCurrentToken();
        return currentTokenType == IdlToken.IDENTIFIER
               && currentTokenEnd > currentTokenStart
               && parser.input().charAt(currentTokenStart) == c;
    }

    /**
     * Expects that the lexeme of the current token is the same as the given characters.
     *
     * @param chars Characters to compare the current lexeme against.
     * @return Returns the current lexeme.
     * @throws ModelSyntaxException if the current lexeme is not equal to the given characters.
     */
    public CharSequence expectCurrentLexeme(CharSequence chars) {
        CharSequence lexeme = getCurrentTokenLexeme();
        boolean isError = lexeme.length() != chars.length();
        if (!isError) {
            for (int i = 0; i < chars.length(); i++) {
                if (lexeme.charAt(i) != chars.charAt(i)) {
                    isError = true;
                    break;
                }
            }
        }
        if (isError) {
            throw syntax("Expected `" + chars + "`, but found `" + lexeme + "`", getCurrentTokenLocation());
        }
        return lexeme;
    }

    /**
     * Assert that the current token is {@code token}.
     *
     * <p>The tokenizer is not advanced after validating the current token.</p>
     *
     * @param token Token to expect.
     * @throws ModelSyntaxException if the current token is unexpected.
     */
    public void expect(IdlToken token) {
        getCurrentToken();

        if (currentTokenType != token) {
            throw syntax(createExpectMessage(token), getCurrentTokenLocation());
        }
    }

    /**
     * Assert that the current token is one of {@code tokens}.
     *
     * <p>The tokenizer is not advanced after validating the current token.</p>
     *
     * @param tokens Assert that the current token is one of these tokens.
     * @return Returns the current token.
     * @throws ModelSyntaxException if the current token is unexpected.
     */
    public IdlToken expect(IdlToken... tokens) {
        getCurrentToken();

        if (currentTokenType == IdlToken.ERROR) {
            throw syntax(createExpectMessage(tokens), getCurrentTokenLocation());
        }

        for (IdlToken token : tokens) {
            if (currentTokenType == token) {
                return token;
            }
        }

        throw syntax(createExpectMessage(tokens), getCurrentTokenLocation());
    }

    private String createExpectMessage(IdlToken... tokens) {
        StringBuilder result = new StringBuilder();
        if (currentTokenType == IdlToken.ERROR) {
            result.append(getCurrentTokenError());
        } else if (tokens.length == 1) {
            result.append("Expected ")
                    .append(tokens[0].getDebug())
                    .append(" but found ")
                    .append(getCurrentToken().getDebug(getCurrentTokenLexeme()));
        } else {
            result.append("Expected one of ");
            for (IdlToken token : tokens) {
                result.append(token.getDebug()).append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append("; but found ").append(getCurrentToken().getDebug(getCurrentTokenLexeme()));
        }
        return result.toString();
    }

    /**
     * Expect that one or more spaces are found at the current token, and skip over them.
     *
     * @throws ModelSyntaxException if the current token is not a space.
     */
    public void expectAndSkipSpaces() {
        if (getCurrentToken() != IdlToken.SPACE) {
            throw syntax("Expected one or more spaces, but found "
                         + getCurrentToken().getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
        skipSpaces();
    }

    private ModelSyntaxException syntax(String message, SourceLocation location) {
        return new ModelSyntaxException("Syntax error at line " + location.getLine() + ", column "
                                        + location.getColumn() + ": " + message, location);
    }

    /**
     * Expect that one or more whitespace characters or documentation comments are found at the current token, and
     * skip over them.
     *
     * @throws ModelSyntaxException if the current token is not whitespace.
     */
    public void expectAndSkipWhitespace() {
        if (!getCurrentToken().isWhitespace()) {
            throw syntax("Expected one or more whitespace characters, but found "
                         + getCurrentToken().getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
        skipWsAndDocs();
    }

    /**
     * Expects that the current token is zero or more spaces/commas followed by a newline, comment, documentation
     * comment, or EOF.
     *
     * <p>If a documentation comment is detected, the current token remains the documentation comment. If an EOF is
     * detected, the current token remains the EOF token. If a comment, newline, or whitespace are detected, they are
     * all skipped, leaving the current token the next token after encountering the matched token. Other kinds of
     * tokens will raise an exception.
     *
     * <p>This method mimics the {@code br} production from the Smithy grammar. When this method is called, any
     * previously parsed and buffered documentation comments are discarded.
     *
     * @throws ModelSyntaxException if the current token is not a newline or followed by a newline.
     */
    public void expectAndSkipBr() {
        // Skip spaces and commas leading up to a required newline.
        getCurrentToken();
        while (currentTokenType.canSkipBeforeBr()) {
            next();
        }

        // The following tokens are allowed tokens that contain newlines.
        switch (getCurrentToken()) {
            case NEWLINE:
            case COMMENT:
            case DOC_COMMENT:
                clearDocCommentLinesForBr();
                next();
                skipWs();
                break;
            case EOF:
                break;
            default:
                throw syntax("Expected a line break, but found "
                             + getCurrentToken().getDebug(getCurrentTokenLexeme()),
                             getCurrentTokenLocation());
        }
    }

    private void clearDocCommentLinesForBr() {
        if (!docCommentLines.isEmpty()) {
            validationEventListener.accept(LoaderUtils.emitBadDocComment(getCurrentTokenLocation(),
                                                                         removePendingDocCommentLines()));
        }
    }

    private IdlToken singleCharToken(IdlToken type) {
        parser.skip();
        currentTokenEnd = parser.position();
        return currentTokenType = type;
    }

    private IdlToken tokenizeNewline() {
        parser.skip(); // this will \n and \r\n.
        currentTokenEnd = parser.position();
        return currentTokenType = IdlToken.NEWLINE;
    }

    private IdlToken tokenizeSpace() {
        parser.consumeWhile(c -> c == ' ' || c == '\t');
        currentTokenEnd = parser.position();
        return currentTokenType = IdlToken.SPACE;
    }

    private IdlToken parseColon() {
        parser.skip();

        if (parser.peek() == '=') {
            parser.skip();
            currentTokenType = IdlToken.WALRUS;
        } else {
            currentTokenType = IdlToken.COLON;
        }

        currentTokenEnd = parser.position();
        return currentTokenType;
    }

    private IdlToken parseComment() {
        // first "/".
        parser.expect('/');

        // A standalone forward slash is an error.
        if (parser.peek() != '/') {
            currentTokenError = "Expected a '/' to follow '/' to form a comment.";
            return singleCharToken(IdlToken.ERROR);
        }

        // Skip the next "/".
        parser.expect('/');

        IdlToken type = IdlToken.COMMENT;

        // Three "///" is a documentation comment.
        if (parser.peek() == '/') {
            parser.expect('/');
            type = IdlToken.DOC_COMMENT;
            // When capturing the doc comment lexeme, skip a single leading space if found.
            if (parser.peek() == ' ') {
                parser.skip();
            }
            int lineStart = parser.position();
            parser.consumeRemainingCharactersOnLine();
            docCommentLines.add(getInput(lineStart, parser.position()));
        } else {
            parser.consumeRemainingCharactersOnLine();
        }

        // Include the newline in the comment and doc comment lexeme.
        if (parser.expect('\r', '\n', SimpleParser.EOF) == '\r' && parser.peek() == '\n') {
            parser.skip();
        }

        currentTokenEnd = parser.position();
        return currentTokenType = type;
    }

    /**
     * Removes any buffered documentation comment lines, and returns a concatenated string value.
     *
     * @return Returns the combined documentation comment string for the given lines. Returns null if no lines.
     */
    String removePendingDocCommentLines() {
        if (docCommentLines.isEmpty()) {
            return null;
        } else {
            StringBuilder result = new StringBuilder();
            result.append(docCommentLines.removeFirst());
            while (!docCommentLines.isEmpty()) {
                result.append('\n').append(docCommentLines.removeFirst());
            }
            return result.toString();
        }
    }

    private IdlToken parseNumber() {
        try {
            String lexeme = ParserUtils.parseNumber(parser);
            if (lexeme.contains("e") || lexeme.contains("E")  || lexeme.contains(".")) {
                double value = Double.parseDouble(lexeme);
                if (Double.isFinite(value)) {
                    currentTokenNumber = value;
                } else {
                    currentTokenNumber = new BigDecimal(lexeme);
                }
            } else {
                try {
                    currentTokenNumber = Long.parseLong(lexeme);
                } catch (NumberFormatException e) {
                    currentTokenNumber = new BigInteger(lexeme);
                }
            }

            currentTokenEnd = parser.position();
            return currentTokenType = IdlToken.NUMBER;
        } catch (RuntimeException e) {
            currentTokenEnd = parser.position();
            // Strip off the leading error message information if present.
            if (e.getMessage().startsWith("Syntax error")) {
                currentTokenError = e.getMessage().substring(e.getMessage().indexOf(':') + 1).trim();
            } else {
                currentTokenError = e.getMessage();
            }
            return currentTokenType = IdlToken.ERROR;
        }
    }

    private IdlToken parseIdentifier() {
        try {
            ParserUtils.consumeIdentifier(parser);
            currentTokenType = IdlToken.IDENTIFIER;
        } catch (RuntimeException e) {
            currentTokenType = IdlToken.ERROR;
            currentTokenError = e.getMessage();
        }
        currentTokenEnd = parser.position();
        return currentTokenType;
    }

    private IdlToken parseString() {
        parser.skip(); // skip first quote.

        if (parser.peek() == '"') {
            parser.skip(); // skip second quote.
            if (parser.peek() == '"') { // A third consecutive quote is a TEXT_BLOCK.
                parser.skip();
                return parseTextBlock();
            } else {
                // Empty string.
                currentTokenEnd = parser.position();
                currentTokenStringSlice = "";
                return currentTokenType = IdlToken.STRING;
            }
        }

        try {
            // Parse the contents of a quoted string.
            currentTokenStringSlice = parseQuotedTextAndTextBlock(false);
            currentTokenEnd = parser.position();
            return currentTokenType = IdlToken.STRING;
        } catch (RuntimeException e) {
            currentTokenEnd = parser.position();
            currentTokenError = "Error parsing quoted string: " + e.getMessage();
            return currentTokenType = IdlToken.ERROR;
        }
    }

    private IdlToken parseTextBlock() {
        try {
            currentTokenStringSlice = parseQuotedTextAndTextBlock(true);
            currentTokenEnd = parser.position();
            return currentTokenType = IdlToken.TEXT_BLOCK;
        } catch (RuntimeException e) {
            currentTokenEnd = parser.position();
            currentTokenError = "Error parsing text block: " + e.getMessage();
            return currentTokenType = IdlToken.ERROR;
        }
    }

    // Parses both quoted_text and text_block
    private CharSequence parseQuotedTextAndTextBlock(boolean triple) {
        int start = parser.position();

        while (!parser.eof()) {
            char next = parser.peek();
            if (next == '"' && (!triple || (parser.peek(1) == '"' && parser.peek(2) == '"'))) {
                // Found closing quotes of quoted_text and/or text_block
                break;
            }
            parser.skip();
            if (next == '\\') {
                parser.skip();
            }
        }

        // Strip the ending '"'.
        CharSequence result = parser.borrowSliceFrom(start);
        parser.expect('"');

        if (triple) {
            parser.expect('"');
            parser.expect('"');
        }

        return IdlStringLexer.scanStringContents(result, triple);
    }
}
