$version: "2.0"

namespace smithy.example

@a
@b("hello")
string Foo

@trait
structure a {}

@trait
string b

@trait
string c
