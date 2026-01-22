$version: "2"

namespace smithy.example

use smithy.contracts#conditions

@conditions({
    NonEmpty: {
        expression: "min == null || max == null || min <= max"
    }
})
@trait(selector: ":test(number, member > number)")
structure range2 {
    min: BigDecimal
    max: BigDecimal
}

@range2(min: 1)
integer Okay

@range2(max: 2)
integer Yup

@range2(min: 1, max: 2)
integer Alrighty

@range2(min: 2, max: 1)
integer Whoops


