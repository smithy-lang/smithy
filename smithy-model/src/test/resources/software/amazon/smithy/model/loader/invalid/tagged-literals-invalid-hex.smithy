// Odd number of hex digits in hex string
$version: "2.1"

namespace smithy.example

structure Foo {
    @default(#hex "abc")
    data: Blob
}
