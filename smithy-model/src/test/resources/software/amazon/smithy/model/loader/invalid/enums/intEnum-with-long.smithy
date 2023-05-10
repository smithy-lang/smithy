// smithy.example#Foo$BAR: intEnum members must fit within an integer, but found: 2147483648
$version: "2.0"

namespace smithy.example

intEnum Foo {
    // one more than the max integer value of 2147483647
    BAR = 2147483648
}
