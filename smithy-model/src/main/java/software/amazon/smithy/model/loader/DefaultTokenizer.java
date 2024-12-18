/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.NoSuchElementException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.utils.SimpleParser;

class DefaultTokenizer implements IdlTokenizer {

    private final String filename;
    private final SimpleParser parser;
    private IdlToken currentTokenType;
    private int currentTokenStart = -1;
    private int currentTokenEnd = -1;
    private int currentTokenLine = -1;
    private int currentTokenColumn = -1;
    private Number currentTokenNumber;
    private CharSequence currentTokenStringSlice;
    private String currentTokenError;

    DefaultTokenizer(String filename, CharSequence model) {
        this.filename = filename;
        this.parser = new SimpleParser(model, 64);
    }

    @Override
    public final String getSourceFilename() {
        return filename;
    }

    @Override
    public final CharSequence getModel() {
        return parser.input();
    }

    @Override
    public final int getPosition() {
        return parser.position();
    }

    @Override
    public final int getLine() {
        return parser.line();
    }

    @Override
    public final int getColumn() {
        return parser.column();
    }

    @Override
    public final IdlToken getCurrentToken() {
        if (currentTokenType == null) {
            next();
        }
        return currentTokenType;
    }

    @Override
    public final int getCurrentTokenLine() {
        getCurrentToken();
        return currentTokenLine;
    }

    @Override
    public final int getCurrentTokenColumn() {
        getCurrentToken();
        return currentTokenColumn;
    }

    @Override
    public final int getCurrentTokenStart() {
        getCurrentToken();
        return currentTokenStart;
    }

    @Override
    public final int getCurrentTokenEnd() {
        return currentTokenEnd;
    }

    @Override
    public final CharSequence getCurrentTokenStringSlice() {
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

    @Override
    public final Number getCurrentTokenNumberValue() {
        getCurrentToken();
        if (currentTokenNumber == null) {
            throw syntax("The current token must be number but found: "
                    + currentTokenType.getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
        return currentTokenNumber;
    }

    @Override
    public final String getCurrentTokenError() {
        getCurrentToken();
        if (currentTokenType != IdlToken.ERROR) {
            throw syntax("The current token must be an error but found: "
                    + currentTokenType.getDebug(getCurrentTokenLexeme()), getCurrentTokenLocation());
        }
        return currentTokenError == null ? "" : currentTokenError;
    }

    @Override
    public final boolean hasNext() {
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

    private ModelSyntaxException syntax(String message, SourceLocation location) {
        return new ModelSyntaxException("Syntax error at line " + location.getLine() + ", column "
                + location.getColumn() + ": " + message, location);
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
        parser.skip();

        IdlToken type = IdlToken.COMMENT;

        // Three "///" is a documentation comment.
        if (parser.peek() == '/') {
            parser.skip();
            type = IdlToken.DOC_COMMENT;
        }

        parser.consumeRemainingCharactersOnLine();

        // Include the newline in the comment and doc comment lexeme.
        if (parser.expect('\r', '\n', SimpleParser.EOF) == '\r' && parser.peek() == '\n') {
            parser.skip();
        }

        currentTokenEnd = parser.position();
        return currentTokenType = type;
    }

    private IdlToken parseNumber() {
        try {
            String lexeme = ParserUtils.parseNumber(parser);
            if (lexeme.contains("e") || lexeme.contains("E") || lexeme.contains(".")) {
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
