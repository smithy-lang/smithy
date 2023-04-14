// Syntax error at line 6, column 17: @default assignment is only supported in IDL version 2 or later | Model
$version: "1.0"
namespace smithy.example

structure Foo {
    baz: String = "hello"
}
