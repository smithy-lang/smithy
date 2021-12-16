$version: "2.0"

namespace com.foo

@trait
string foo

@trait
boolean baz

@trait
integer qux

@trait
union bam {
    entry: String,
}

@trait
structure struct {}

// A null trait value cannot be coerced to a string.
@foo
// A null trait value cannot be coerced to a boolean.
@baz
// A null trait value cannot be coerced to an integer.
@qux
// A null trait value cannot be coerced to a union.
@bam
// Can't pass true for a structure trait
@struct(true)
string MyString

// Can't pass null for a structure trait
@struct(null)
string MyString2
