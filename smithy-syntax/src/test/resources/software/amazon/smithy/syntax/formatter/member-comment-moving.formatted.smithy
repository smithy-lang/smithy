$version: "2.0"

namespace smithy.example

// Issue 1: Trailing comment on same line as member gets moved with extra blank line.
structure Issue1 {
    a: String // a
    b: String
}

// Issue 1b: Same as issue 1, but no next member.
structure Issue1b {
    a: String // a
}

// Issue 1c: Trailing comment between two members.
structure Issue1c {
    a: String // a
    b: String // b
    c: String
}

// Issue 2: Default value assignment with trailing comment on next line gets extra blank lines.
structure Issue2 {
    a: String = ""
    // a
    b: String
}

// Issue 2b: Same as issue 2, but no next member.
structure Issue2b {
    a: String = ""
    // a
}

// Issue 2c: Default value assignment with trailing comment on same line.
structure Issue2c {
    a: String = "" // a
    b: String
}

// Issue 3: Doc comment after default value assignment is not fixed to regular comment.
structure Issue3a {
    a: String = "" // a
    b: String
}

// Issue 3b: doc comment on the line after a default value, with a following member.
// `/// a` is `b`'s doc comment per Smithy IDL rules and must be preserved as-is.
structure Issue3b {
    a: String = ""
    /// a
    b: String
}

// Issue 3c: Doc comment trailing on same line as member (no default).
structure Issue3c {
    a: String // a
    b: String
}

// Issue 4: Trailing comment on member with trait applied.
structure Issue4 {
    @required
    a: String // a

    b: String
}

// Issue 4b: Trailing comment on member with trait and default value.
structure Issue4b {
    @required
    a: String = "" // a

    b: String
}

// Issue 5: Enum members with value assignments and trailing comments.
enum Issue5 {
    A = "a" // a
    B = "b"
}

// Issue 5b: Enum member with value assignment and comment on next line.
enum Issue5b {
    A = "a"
    // a
    B = "b"
}

// Issue 5c: Enum member with value assignment, doc comment trailing (should convert).
enum Issue5c {
    A = "a" // a
    B = "b"
}

// Issue 6: Multiple consecutive comments after a VALUE_ASSIGNMENT.
structure Issue6 {
    a: String = ""

    // comment 1
    // comment 2
    b: String
}

// Issue 6b: Multiple consecutive comments after VALUE_ASSIGNMENT, last member.
structure Issue6b {
    a: String = ""

    // comment 1
    // comment 2
}

// Issue 7: All members have VALUE_ASSIGNMENT with trailing comments, no traits.
// Should use single-line separation (no blank lines between members).
structure Issue7 {
    a: String = "" // a
    b: String = "" // b
    c: String = ""
}

// Issue 8: Nested inline aggregate shape with member comments.
operation Issue8 {
    input := {
        a: String = "" // a
        b: String
    }
}

// Issue 9: Elided member with trailing comment (no VALUE_ASSIGNMENT).
@mixin
structure Issue9Base {
    a: String
}

structure Issue9 with [Issue9Base] {
    $a // elided with comment
    b: String
}

// Issue 10: Nested inline shape with doc comments on inner members.
operation Issue10 {
    input := {
        /// Doc comment on inner member.
        a: String

        b: String
    }
}

// Issue 11: Multi-line value assignment with trailing comment.
structure Issue11 {
    a: String = """
    multi
    line
    """ // a
    b: String
}
