$version: "1.0"

namespace smithy.example

structure Integers {
    noRange: PrimitiveInteger,

    @range(min: 0, max: 1)
    valid: PrimitiveInteger,

    @range(min: 1)
    invalidMin: PrimitiveInteger,

    @range(max: -1)
    invalidMax: PrimitiveInteger,
}
