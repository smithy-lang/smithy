// Syntax error at line 6, column 24: Expected a line break, but found IDENTIFIER('baz') | Model
$version: "2.0"
namespace com.foo

structure Foo {
    bar: String = "Hi" baz: String = "Hi"
}
