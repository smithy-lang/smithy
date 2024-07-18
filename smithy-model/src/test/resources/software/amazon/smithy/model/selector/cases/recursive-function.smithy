$version: "2.0"

metadata selectorTests = [
    {
        // Get the resource hierarchy of a shape.
        selector: "[id=smithy.example#C] :recursive(<-[resource]-)"
        matches: [
            smithy.example#A
            smithy.example#B
        ]
    }

    {
        // Check if a shape is in a specific heirarchy.
        selector: ":test(:recursive(<-[resource]-) [id=smithy.example#A])"
        matches: [
            smithy.example#B
            smithy.example#C
        ]
    }

    {
        // Get all mixins of a shape.
        selector: "[id=smithy.example#Indirect2] :recursive(-[mixin]->)"
        matches: [
            smithy.example#Indirect
            smithy.example#Direct
            smithy.example#MyMixin
        ]
    }

    {
        // Get all shapes that have a specific mixin.
        // This will also short-circuit the recursive function once the fist match is found by the attribute.
        selector: ":test(:recursive(-[mixin]->) [id=smithy.example#MyMixin])"
        matches: [
            smithy.example#Direct
            smithy.example#Indirect
            smithy.example#Indirect2
        ]
    }
    {
        // This is the same as the previous selector, but uses and unnecessary :test in the :test.
        // This will also short-circuit the recursive function once the fist match is found by the test.
        selector: ":test(:recursive(-[mixin]->) :test([id=smithy.example#MyMixin]))"
        matches: [
            smithy.example#Direct
            smithy.example#Indirect
            smithy.example#Indirect2
        ]
    }

    {
        // An inefficient way to check if a mixin is applied to any shape as a mixin.
        // The more efficient approach is: [trait|mixin] :test(<-[mixin]-)
        selector: ":recursive(-[mixin]->) [trait|mixin]"
        matches: [
            smithy.example#MyMixin
            smithy.example#Direct
            smithy.example#Indirect
        ]
    }
    {
        // Proof of the more efficient way to check if a mixin is applied to any shape as a mixin.
        selector: "[trait|mixin] :test(<-[mixin]-)"
        matches: [
            smithy.example#MyMixin
            smithy.example#Direct
            smithy.example#Indirect
        ]
    }


    {
        // A slightly less efficient form of "~>".
        selector: "[id=smithy.example#Direct] :recursive(>)"
        matches: [
            smithy.example#MyMixin
            smithy.api#String
            smithy.example#Direct$foo
        ]
    }
    {
        // This is the same result, but slightly more efficient.
        selector: "[id=smithy.example#Direct] ~>"
        matches: [
            smithy.example#MyMixin
            smithy.api#String
            smithy.example#Direct$foo
        ]
    }

    {
        // Find the closure of shapes that ultimately target a specific shape.
        selector: "[id=smithy.example#Direct] :recursive(<)"
        matches: [
            smithy.example#Indirect
            smithy.example#Indirect2
        ]
    }

    {
        // Make a pathological selector to ensure we don't inifinitely recurse.
        // This just matches shapes in the smithy.example namespace that are targeted by another shape.
        // Note: This isn't a useful selector.
        selector: ":recursive(:recursive(:recursive(:recursive(:recursive(~>))))) [id|namespace=smithy.example]"
        matches: [
            smithy.example#C
            smithy.example#Direct
            smithy.example#MyMixin
            smithy.example#Indirect
            smithy.example#B
            smithy.example#Direct$foo
            smithy.example#Indirect$foo
            smithy.example#Indirect2$foo
        ]
    }
    {
        // Make another pathological selector to ensure we don't inifinitely recurse.
        // This matches shapes in the smithy.example namespace that are targeted by another shape that are targeted
        // by another shape. Note: This isn't a useful selector.
        selector: "~> :recursive(:recursive(:recursive(:recursive(:recursive(~>))))) [id|namespace=smithy.example]"
        matches: [
            smithy.example#C
            smithy.example#Direct
            smithy.example#MyMixin
            smithy.example#Direct$foo
            smithy.example#Indirect$foo
        ]
    }
]

namespace smithy.example

resource A {
    resources: [B]
}

@internal
resource B {
    resources: [C]
}

resource C {}

@mixin
structure UnusedMixin {}

@mixin
structure MyMixin {}

@mixin
structure Direct with [MyMixin] {
    foo: String
}

@mixin
structure Indirect with [Direct] {}

structure Indirect2 with [Indirect] {}
