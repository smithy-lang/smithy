// smithy.example#Foo: Syntax error at line 8, column 5: Found conflicting member name, `BAR`
$version: "2.0"

namespace smithy.example

enum Foo {
    BAR = "bar"
    BAR = "bar"
}
