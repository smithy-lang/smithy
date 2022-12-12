// The documentation comment in the enum is within the scope of defining a shape.
// Because it's out of position and not before anything that consumes it, it
// should not be applied to the next shape definition, Foo. Applying it to Foo
// would not only be invalid, it would also cause a conflict with its actual
// documentation trait.
$version: "2.0"

namespace smithy.example

@enum([
    /// Invalid!
    {
        name: "X"
        value: "X"
    }
])
string Features

@documentation("X")
map Foo {
    key: Features
    value: Boolean
}

