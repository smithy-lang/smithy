$version: "2.1"

namespace smithy.example

structure Indexed {
    @idx(1)
    unresolved: String

    @smithy.protocols#idx(0)
    zero: String

    @smithy.protocols#idx(1.5)
    decimal: String

    @smithy.protocols#idx(2147483648)
    overflow: String
}
