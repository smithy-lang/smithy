namespace com.example

// Single trait
@sensitive
string A

// Multiple traits same lines
@sensitive
@deprecated
string B

// Multiple traits separate lines
@sensitive
@deprecated
string C

// Multiple spaces between.
@sensitive
@deprecated
string D

// String
@documentation("Hello!")
string E

// Multiple lines and escapes
@documentation("
Hello! This is a test.

Ignore these tokens: {}[](),:->$version//<> +10 -10 =

Is it working? Is \"This\" the 'expected' result?
Is this a backslash? \"\\\".")
string F

// Unquoted string resolves to a shape ID
@documentation(H)
string H

// Unquoted string array
@references([{
    resource: Foo
}, {
    resource: Foo
    rel: "baz"
}])
string I

resource Foo {
    identifiers: {
        abc: smithy.api#String
    }
}

// Quoted string array
@references([{
    "resource": "com.example#Foo"
}, {
    "resource": "com.example#Foo"
    "rel": "baz"
}])
string J

// Empty object
@deprecated()
string K

// Empty list
@tags([])
string L

// List with values
@tags([

    "a"
    "b"
    "c"
])
string M

// List with values and trailing comma
@tags([

    "a"
    "b"
    "c"
])
string N

// List with quoted values.
@tags([

    "a"
    "b"
    "c"
])
string O

// List with quoted values spanning multiple lines.
@tags([

    "a"
    "b"
    "c"
])
string P

// Boolean
@sensitive
string Q

// Number
@trait
integer numeric

@com.example#numeric(100)
string R

// Coerce into object trait.
@references
string S

// Coerce into array trait.
@tags
string T

// Long strings
@documentation("""
    This is a
    string defined on multiple lines.
    It \\
    can \\
    span
    a
    great
    many
    lines.""")
string U

apply E @deprecated
@documentation("")
string V

@trait
bigDecimal bDecimal

@com.example#bDecimal(9223372036854775808)
string W

@com.example#bDecimal(1.7976931348623157E+309)
string X

@com.example#bDecimal(2e+308)
string Y

@com.example#bDecimal(2E+308)
string Z

@trait
bigInteger bInteger

@com.example#bInteger(9223372036854775808)
string ZA

