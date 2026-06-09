# Tagged string literals

* **Author**: Manuel Sugawara
* **Created**: 2026-05-14

## Abstract

This proposal introduces tagged string literals to the Smithy IDL. A tagged string literal is a string prefixed with `#<tag>` that changes how the string content is interpreted at parse time. The tag instructs the parser to use alternative rules for processing escape sequences and encoding, producing a regular string value. This is purely syntactic sugar; the AST and semantic model remain unchanged. Four initial tags are proposed: `#re` (regex), `#b` (binary), `#hex` (hex dump), and `#timestamp` (ISO date to epoch seconds).

## Motivation

Smithy string literals use standard escape sequences (`\\n`, `\\t`, `\\uXXXX`, etc.). This works well for general text, but becomes cumbersome for certain domains:

**Regular expressions** require heavy escaping. A simple pattern like `^\d{5}$` must be written as `"^\\d{5}$"` because `\d` is not a valid Smithy escape sequence. This double-escaping is error-prone and hard to read.

**Binary data** in Smithy is represented as base64-encoded strings (for blob shapes). Authors must manually encode binary values, making it difficult to express byte sequences directly.

**Hex data** is common in protocol specifications and test fixtures. Expressing raw bytes as base64 obscures the actual byte values that protocol authors are working with.

**Timestamps** as epoch seconds are opaque. A value like `1704067200` gives no indication that it represents January 1, 2024 without external tooling.

These are common enough patterns that dedicated syntax would significantly improve readability and reduce errors.

## Proposal

### Syntax

A tagged string literal consists of `#` followed by a tag identifier, followed by a string literal or text block:

```
#re "^\d{5}$"
#b "Hello world"
#hex "48 65 6c 6c 6f"
#timestamp "2024-01-01T00:00:00Z"
#re """
    ^\d{5}$
    """
```

The tag identifier follows the same rules as Smithy identifiers (letters, digits, underscores; must start with a letter or underscore).

#### ABNF

```
tagged_string_literal = "#" identifier (quoted_text / text_block)

quoted_text = DQUOTE *quoted_char DQUOTE

text_block = three_dquotes br *text_block_content three_dquotes
```

A tagged string literal can appear anywhere a regular string literal can appear in the IDL (trait values, node values, etc.).

### Disambiguation

The `#` character is already used in shape IDs (e.g., `com.example#Shape`). Disambiguation between shape ID fragments and tagged string literals is context-dependent. In positions where the grammar expects a shape ID (e.g., after `apply`, in `use` statements, or as trait targets), `#` followed by an identifier is parsed as a shape ID member. In positions where a value is expected (trait arguments, node values, default values), `#` followed by a known tag and a string literal is parsed as a tagged string literal. In all other cases, `#` is emitted as a `POUND` token as before.

### Processing model

Tagged string literals are processed entirely at the syntax level:

1. The tokenizer recognizes the `#tag` prefix.
2. The string content is extracted (handling quotes and text block formatting).
3. A tag-specific scanner processes the raw content, applying its own escape and encoding rules.
4. The result is emitted as a regular `STRING` token. The `#timestamp` tag is an exception: it emits a `NUMBER` token rather than a `STRING` token.

No information about the tag is preserved in the AST or semantic model. The tagged literal and its equivalent plain string are indistinguishable after parsing.

### Initial tags

#### `#re` Regular expression literals

The `#re` tag treats backslash sequences as literal characters rather than escape sequences. This allows regex patterns to be written naturally without double-escaping.

**Escape rules:**

* `\"` → produces a literal double quote (necessary to include `"` in the string)
* `\\` → produces a literal backslash
* All other `\X` sequences → passed through literally as `\X` (two characters)

**Examples:**

|Tagged literal|Equivalent plain string|Regex meaning|
|-|-|-|
|`#re "^\d{5}$"`|`"^\\d{5}$"`|Match 5 digits|
|`#re "\w+"`|`"\\w+"`|One or more word chars|
|`#re "a\"b"`|`"a\"b"`|Literal `a"b`|
|`#re "a\\b"`|`"a\\b"`|Literal `a\b`|

#### `#b` Binary (byte) literals

The `#b` tag interprets the string as a sequence of bytes, similar to Python's `b'...'` syntax. The string can contain hex escapes (`\xHH`), octal escapes (`\OOO`), or literal character values. The resulting token is the base64-encoded representation of the bytes. The escape rules are designed to be compatible with Python's byte string literals, so authors can copy escape sequences between Smithy models and Python code without modification.

**Escape rules:**

* `\xHH` → produces the byte with hex value `HH`
* `\OOO` → produces the byte with octal value `OOO`
* `\\` → produces a backslash byte (0x5C)
* `\"` → produces a double quote byte (0x22)
* `\a` → produces a bell byte (0x07)
* `\b` → produces a backspace byte (0x08)
* `\f` → produces a form feed byte (0x0C)
* `\n` → produces a newline byte (0x0A)
* `\r` → produces a carriage return byte (0x0D)
* `\t` → produces a tab byte (0x09)
* `\v` → produces a vertical tab byte (0x0B)
* `\0` → produces a null byte (0x00)
* All other characters → encoded as their UTF-8 byte representation

**Examples:**

|Tagged literal|Equivalent plain string|Meaning|
|-|-|-|
|`#b "Hello world"`|`"SGVsbG8gd29ybGQ="`|UTF-8 bytes, base64-encoded|
|`#b "\x00\x01\x02"`|`"AAEC"`|Three bytes: 0, 1, 2|
|`#b "\xff"`|`"/w=="`|Single byte: 255|

#### `#hex` Hex dump literals

The `#hex` tag interprets the string as a hex dump. The string contains hexadecimal values where spaces are ignored and lines starting with `#` are treated as comments (ignored until end of line). The resulting token is the base64-encoded representation of the decoded bytes.

This is particularly useful for protocol tests where binary formats like CBOR are specified using annotated hex dumps. Authors can paste annotated CBOR directly from specifications or diagnostic tools:

```smithy
body: #hex """
    81                        # array(1)
       c1                     #   epoch datetime value, tag(1)
          fb 41d9ad970f9b4396 #     float(1,723,227,198.426)
                              #     datetime(2024-08-09T18:13:18.426000118Z)
    """
```

**Rules:**

* Hex digits (`0-9`, `a-f`, `A-F`) are read in pairs to produce bytes
* Spaces and tabs are ignored (for readability)
* Lines starting with `#` are comments and ignored until end of line
* Newlines are ignored

**Examples:**

```smithy
@example(#hex "48656c6c6f")
```

Equivalent to `"SGVsbG8="` (base64 of "Hello").

```smithy
@example(#hex """
    # File header
    89 50 4e 47 0d 0a 1a 0a
    # IHDR chunk
    00 00 00 0d 49 48 44 52
    """)
```

#### `#timestamp` Timestamp literals

The `#timestamp` tag converts an ISO 8601 date/time string into its `epoch-seconds` representation, following the same rules as the `epoch-seconds` timestamp format defined by Smithy. This makes timestamp default values and test data more readable.

**Rules:**

* Input must be a valid ISO 8601 date/time string (as defined by the `date-time` production in RFC 3339 Section 5.6)
* Output is the number of seconds that have elapsed since 00:00:00 Coordinated Universal Time (UTC), Thursday, 1 January 1970, with optional millisecond precision. Values that are more granular than millisecond precision SHOULD be truncated to fit millisecond precision. This follows the same rules as the `epoch-seconds` timestamp format.

**Examples:**

|Tagged literal|Equivalent value|Meaning|
|-|-|-|
|`#timestamp "2024-01-01T00:00:00Z"`|`1704067200`|Midnight Jan 1, 2024 UTC|
|`#timestamp "2024-06-15T12:30:00.500Z"`|`1718451000.5`|With milliseconds|

### Text block support

Both tags work with text blocks. The text block formatting rules (leading whitespace removal, line normalization) are applied before the tag-specific scanner processes the content:

```smithy
@pattern(#re """
    ^\d{5}(-\d{4})?$
    """)
string ZipCode
```

### Error handling

* **Unknown tag**: If a `#` is followed by an identifier that is not a recognized tag, the parser emits an error: `Unknown tagged literal: #foo`
* **Invalid escapes**: Tag-specific escape errors produce clear messages, e.g., `Incomplete \x escape in binary string`
* **Missing string**: If `#tag` is not followed by a string literal, the parser emits: `Expected a string literal after #tag`

Because the tag set is closed and tied to a specific Smithy IDL version, models using newer tags will fail to parse on older tooling. This is consistent with how other IDL version features are handled.

## Alternatives considered

### Python-style prefix literals (`b"..."`)

Using a single-letter prefix directly attached to the string, like Python's `b'...'` or Rust's `r"..."`.

This was rejected because:

* It only solves one use case at a time. Binary gets `b"..."`, but regex would need a separate mechanism (e.g., `r"..."`) — each one is a new special case.
* A letter followed by a string is ambiguous with an identifier followed by a string value, requiring context-sensitive parsing.
* Not extensible without grammar changes — each new prefix is a new production.
* The tagged literal approach (`#tag "..."`) provides a single, general mechanism that handles binary, regex, and future tags uniformly.

### Prefix character other than `#`

Using a different prefix like `` ` `` (backtick) or `r` (like Rust raw strings).

This was rejected because:

* `#` is already meaningful in Smithy and visually signals "special processing."
* Backtick would introduce a new character with no precedent in the IDL.
* A bare letter prefix (like `r"..."`) is ambiguous with identifiers.

### Raw strings (no escape processing at all)

A raw string mode where no escapes are processed, similar to Python's `r"..."`.

This was rejected because:

* It doesn't solve the binary literal use case.
* The tag system is more general and extensible.
* For regex specifically, `\"` still needs to work (to include quotes), so truly "raw" strings aren't practical.

### Extensible/user-defined tags

Allowing model authors to define custom tags via plugins or configuration.

This was rejected for the initial implementation because:

* It adds significant complexity (tag registration, validation, portability).
* The four initial tags cover the most pressing use cases.
* The design does not preclude adding extensibility later.

### AST-level representation

Preserving the tag information in the AST (e.g., as a node annotation).

This was rejected because:

* Tags are purely about authoring convenience, not semantics.
* Preserving them would complicate every tool that processes the AST.
* The AST already has a well-defined string representation; adding variants
  would break existing consumers.

## Limitations

* Tagged string literals are IDL-only. They cannot be expressed in the JSON AST format.
* The set of tags is fixed and hardcoded. New tags require changes to the Smithy parser.
* IDL round-tripping: converting AST back to IDL cannot reconstruct tagged literals since the tag information is not preserved. The plain string equivalent is emitted instead.

## Implementation

The implementation modifies `DefaultTokenizer.java` to:

1. Recognize `#` followed by a known tag identifier and a string literal.
2. Consume the tag and string as a single token.
3. Delegate to a tag-specific scanner that applies its own escape rules.
4. Emit the result as a regular `STRING` token.

The tag handlers are hardcoded in a map from tag name to scanner function. The existing `IdlStringLexer.scanStringContents` is bypassed for tagged strings; instead, each tag provides its own scanning logic.

## FAQ

### Can tagged literals be used in trait values?

Yes. A tagged literal can appear anywhere a string value is expected in the IDL.
For example:

```smithy
@pattern(#re "^\d+$")
string NumericString
```

### Can tagged literals span multiple lines?

Yes, via text blocks. Note that newlines are preserved in the output. To continue a pattern across lines without including the newline, escape it with `\` at the end of the line:

```smithy
@pattern(#re """
    ^\d{5}\
    (-\d{4})?$
    """)
string ZipCode
```

This produces the pattern `^\d{5}(-\d{4})?$` (no newline between the parts).

### What happens if a new tag conflicts with a shape name?

Tags are only recognized immediately after `#` when followed by a string literal. Since shape IDs always have a namespace before `#` (e.g., `com.example#Shape`), there is no ambiguity. A standalone `#re` followed by a string is always a tagged literal.

### Can tags be nested or composed?

No. Tagged literals cannot be nested or composed. The content of a tagged string is processed by the tag's scanner and produces a plain string. There is no mechanism for nesting or chaining tags.
