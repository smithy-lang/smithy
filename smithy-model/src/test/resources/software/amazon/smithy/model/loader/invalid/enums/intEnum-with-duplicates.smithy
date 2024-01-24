// smithy.example#Foo: Syntax error at line 8, column 5: Found conflicting member name, `BAR`
$version: "2.0"

namespace smithy.example

intEnum Foo {
    BAR = 10
    BAR = 10
}
