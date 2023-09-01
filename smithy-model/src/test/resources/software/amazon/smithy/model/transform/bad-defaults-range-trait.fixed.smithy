$version: "2.0"

namespace smithy.example

structure Foo {
    // Default was removed.
    @range(min: 1)
    invalid1: Integer

    // Default was removed.
    invalid2: ValueGreaterThanZero

    // The default of the target shape was removed, so it can be removed here too.
    invalid3: ValueGreaterThanZeroWithDefault

    // Cancel out the root level default.
    @range(min: 1)
    invalid4: PrimitiveInteger = null

    valid1: ValueGreaterThanZero = 1

    valid2: ValueGreaterThanZero

    valid3: Integer

    valid4: Integer = 0

    @range(min: 1)
    valid5: Integer

    @range(min: 1)
    valid6: Integer = 1
}

@range(min: 1)
integer ValueGreaterThanZero

@range(min: 1)
integer ValueGreaterThanZeroWithDefault // default was removed
