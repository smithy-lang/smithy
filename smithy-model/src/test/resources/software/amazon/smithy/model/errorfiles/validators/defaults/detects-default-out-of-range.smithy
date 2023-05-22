$version: "2.0"

namespace smithy.example

structure Integers {
    @default(0)
    noRange: PrimitiveInteger,

    @range(min: 0, max: 1)
    @default(0)
    valid: PrimitiveInteger,

    @range(min: 1)
    @default(0)
    invalidMin: PrimitiveInteger,

    @range(max: -1, min: -100)
    @default(0)
    invalidMax: PrimitiveInteger,

    @default(0)
    invalidTargetMin: MinOne,

    @default(0)
    invalidTagetMax: MaxNegativeOne,

    @default(0)
    invalidMinWithMax: MinAndMax,

    @range(min: 1)
    @default(0)
    doublyInvalidDefault: MinOne
}

@range(min: 1)
@default(0)
integer MinOne

@range(min: 1, max: 100)
@default(0)
integer MinAndMax

@range(max: -1)
@default(0)
integer MaxNegativeOne
