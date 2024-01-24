$version: "2.0"

// Single line
metadata a = { a: "b" }

// Multiline
metadata b = {
    a: "foobarbazbux"
    b: "foobarbazbux"
    c: "foobarbazbux"
    d: "foobarbazbux"
    e: "foobarbazbux"
    f: "foobarbazbux"
}

// Leading and trailing WS
metadata c = {
    // Foo
    a: "b"
}

metadata d = {
    a: "b" // Foo
}

// Parent is array
metadata e = [
    {
        foo: "bar"
    }
]

// Line breaking values
metadata f = [
    """
    foo
    """
]

metadata g = [
    "abc
def"
]

metadata h = {
    a: [
        {
            foo: "bar"
        }
    ]

}

namespace smithy.example

// TraitStructure
// Single line
@externalDocumentation(a: "http://foo.com", c: "http://foo.com")
structure A {}

// Multi-line
@externalDocumentation(
    a: "http://foo.com"
    b: "http://foo.com"
    c: "http://foo.com"
    d: "http://foo.com"
    e: "http://foo.com"
    f: "http://foo.com"
    g: "http://foo.com"
    h: "http://foo.com"
    i: "http://foo.com"
    j: "http://foo.com"
)
structure B {}

// Leading WS
@externalDocumentation(
    // Foo
    a: "http://foo.com"
)
structure C {}

// Trailing WS
@externalDocumentation(
    a: "http://foo.com" // Foo
)
structure D {}

// Object values
@foo(
    a: { bar: "baz" }
)
structure E {}

// Array values
@foo(
    a: ["bar", "baz"]
)
structure F {}

// TraitNode
// Single line
@foo(["foo", "bar"])
structure G {}

@foo({ foo: "bar", baz: "foo" })
structure H {}

// Multiline
@foo([
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
    "foobarbaz"
])
structure I {}

@foo({
    a: "foobarbaz"
    b: "foobarbaz"
    c: "foobarbaz"
    d: "foobarbaz"
    e: "foobarbaz"
    f: "foobarbaz"
    g: "foobarbaz"
    h: "foobarbaz"
    i: "foobarbaz"
})
structure J {}

// Leading WS
@foo(
    // Foo
    [
        "a"
    ]
)
structure K {}

// Trailing WS
@foo(
    [
        "a"
    ] // Foo
)
structure L {}

// Line breaking NodeValues
@foo(
    """
    foo
    """
)
structure M {}

@foo(
    "a
bc"
)
structure N {}

// Nested comments
@foo([
    "abc" // Foo
    "def" // Bar
])
structure O {}

// Nested objects
@foo([{}])
structure P {}

@foo([{
    foo: "bar"
      }])
structure Q {}

@foo([[[]]])
structure R {}
