// [ERROR] smithy.example#Foo$BAR: intEnum shapes require integer values but found: "Abc"
$version: "2.0"

namespace smithy.example

intEnum Foo {
    BAR = "Abc"
}
