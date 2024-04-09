$version: "2.0"

namespace smithy.example

structure Foo {
    // The member itself creates an invalid combination of the range trait and default value.
    @range(min: 1)
    invalid1: Integer = 0

    // The member adds a default value that is incompatible with the target shape.
    invalid2: ValueGreaterThanZero = 0

    // The member targets a shape where the range trait is incompatible with the default of the member.
    invalid3: ValueGreaterThanZeroWithDefault = 0

    // The range trait here makes the default value invalid. This member targets a root shape with a default, so the
    // default has to be set to null to cancel out the root level default.
    @range(min: 1)
    invalid4: PrimitiveInteger = 0

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
@default(0) // bad default and range trait combination.
integer ValueGreaterThanZeroWithDefault
