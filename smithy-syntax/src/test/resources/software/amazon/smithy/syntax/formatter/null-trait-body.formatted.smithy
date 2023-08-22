$version: "2.0"

namespace smithy.example

structure Struct {
    @default
    a: String

    @default
    b: String

    @default
    c: String

    @default(null)
    d: String
}
