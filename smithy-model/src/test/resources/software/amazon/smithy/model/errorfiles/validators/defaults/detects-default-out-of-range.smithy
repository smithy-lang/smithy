$version: "1.0"

namespace smithy.example

structure Integers {
    noRange: PrimitiveInteger,

    @range(min: 0, max: 1)
    valid: PrimitiveInteger,

    @range(min: 1)
    invalidMin: PrimitiveInteger,

    @range(max: -1, min: -100)
    invalidMax: PrimitiveInteger,

    invalidTargetMin: MinOne,

    invalidTagetMax: MaxNegativeOne,

    invalidMinWithMax: MinAndMax
}

@range(min: 1)
integer MinOne

@range(min: 1, max: 100)
integer MinAndMax

@range(max: -1)
integer MaxNegativeOne
