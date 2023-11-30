$version: "2.0"

metadata diffEvaluators = [
    {
        id: "AddedInternalOperation"
        message: "Added operation with `@internal` trait."
        emitCondition: "ForEachMatch"
        appliesTo: "AddedShapes"
        severity: "NOTE"
        selector: "operation [trait|internal]"
    }
    {
        id: "AddedOnlyPrimitiveNumbersAndBools"
        message: "All added numbers and booleans were primitives."
        emitCondition: "IfAllMatch"
        appliesTo: "AddedShapes"
        severity: "NOTE"
        filter: ":is(member :test(> number), number)"
        selector: "[trait|default = 0]"
    }
    {
        id: "AddedMemberWithoutClientOptional"
        message: "One of the added members does not have `@clientOptional` trait."
        emitCondition: "IfAnyMatch"
        appliesTo: "AddedShapes"
        severity: "WARNING"
        selector: "member :not([trait|clientOptional])"
    }
    {
        id: "RemovedRootShape"
        message: "Removed root shape."
        emitCondition: "ForEachMatch"
        appliesTo: "RemovedShapes"
        severity: "NOTE"
        selector: "simpleType"
    }
]

namespace smithy.example

@internal
operation InternalOperation {}

structure Foo {
    bar: Integer = 0
    baz: String
}

structure Bar {
    baz: Integer = 0
}

@default(0)
integer PrimitiveInt
