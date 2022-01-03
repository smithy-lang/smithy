$version: "2.0"

namespace smithy.example

structure MixedStructure {
    @required
    @internal
    redefineable: String
}
