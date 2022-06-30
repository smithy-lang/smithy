// [ERROR] smithy.example#Foo$BAR: intEnum shapes do not support floating point values
$version: "2.0"

namespace smithy.example

intEnum Foo {
    BAR = 1.5
}
