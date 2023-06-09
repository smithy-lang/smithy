$version: "2.0"

namespace smithy.example

operation Empty {}

operation OnlyInput {
    input: Struct
}

operation OnlyOutput {
    output: Struct
}

operation InputAndOutput {
    input: Struct
    output: Struct
}

operation InputAndOutputWithComments {
    // A
    input: Struct

    // B
    output: Struct
}

operation InlineEmptyInput {
    input := {}
}

operation InlineEmptyOutput {
    output := {}
}

operation InlineEmptyInputOutput {
    input := {}
    output := {}
}

operation InlineEmptyInputOutputWithResource {
    input := for Foo {}
    output := for Foo {}
}

operation InlineEmptyInputOutputWithResourceAndMixins {
    input := for Foo with [X] {}
    output := for Foo with [X, Y] {}
}

operation InlineEmptyInputOutputWithTraits {
    input :=
        @since("1.0")
        for Foo {}

    output :=
        @since("1.0")
        for Foo {}
}

operation InlineEmptyInputOutputWithComments {
    input :=
        /// Docs 1
        for Foo {}

    output :=
        /// Docs 2
        for Foo {}
}

structure Struct {}

resource Foo {}

@mixin
structure X {}

@mixin
structure Y {}
