namespace com.example

// Single trait

@sensitive
string A

// Multiple traits same lines

@sensitive @deprecated
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

Is it working? Is \"This\" the 'expected' result?
Is this a backslash? \"\\\".")
string F

@documentation('
Hello! This is a test.

Ignore these tokens: {}[](),:->$version//<> +10 -10 =

Is it working? Is \'This\' the "expected" result?
Is this a backslash? \'\\\'.')
string G

// Unquoted string

@documentation(Hello)
string H

// Unquoted string array

@references(test: { resource: Foo }, baz: { resource: Foo, rel: "baz" })
string I
resource Foo {
  identifiers: { abc: smithy.api#String },
}

// Quoted string array

@references("test": { "resource": "Foo" }, "baz": { "resource": "Foo", "rel": "baz" })
string J

// Empty object

@references()
string K

// Empty list

@tags([])
string L

// List with values

@tags([a, b, c])
string M

// List with values and trailing comma

@tags([a, b, c, ])
string N

// List with quoted values.

@tags(['a', "b", 'c'])
string O

// List with quoted values spanning multiple lines.

@tags(
[
'a'
,
"b"
,
'c'
]
)
string P

// Boolean

@sensitive
string Q

// Number

trait numeric {
  shape: smithy.api#Integer,
  selector: "*",
}

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
    It \
    can \
    span
    a
    great
    many
    lines.""")
string U
