$version: "2.0"

namespace com.amazonaws.simple

@trait
@constrainShapes(
    // Valid
    "foo.bar": {
        selector: "string"
        message: "This one is invalid"
    }
    // Valid
    "foo_bar": {
        selector: "string"
        message: "This one is invalid"
    }
    // Valid
    "_foo_bar": {
        selector: "string"
        message: "This one is invalid"
    }
    // Valid
    "Foo.bar": {
        selector: "string"
        message: "This one is invalid"
    }
    // Valid
    "Foo": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure allValid {}


@trait
@constrainShapes(
    // Invalid
    ".": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid1 {}

@trait
@constrainShapes(
    // Invalid
    "foo.": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid2 {}

@trait
@constrainShapes(
    // Invalid
    "_": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid3 {}

@trait
@constrainShapes(
    // Invalid
    "1": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid4 {}

@trait
@constrainShapes(
    // Invalid
    "a.1": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid5 {}

@trait
@constrainShapes(
    // Invalid
    ".a": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid6 {}

@trait
@constrainShapes(
    // Invalid
    "!.a": {
        selector: "string"
        message: "This one is invalid"
    }
)
structure invalid7 {}
