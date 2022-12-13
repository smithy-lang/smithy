// Parse error at line 6, column 24 near `baz`: Expected a line break | Model
$version: "2.0"
namespace com.foo

structure Foo {
    bar: String = "Hi" baz: String = "Hi"
}
