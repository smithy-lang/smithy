// Parse error at line 6, column 16 near `!\n`: The '!' suffix can only be used on structure members when using Smithy 2.0 or later, but you're using version `1.0`. Make `$version: "2"` the first line of this file.
$version: "1"
namespace smithy.example

structure Foo {
    bar: String!
}
