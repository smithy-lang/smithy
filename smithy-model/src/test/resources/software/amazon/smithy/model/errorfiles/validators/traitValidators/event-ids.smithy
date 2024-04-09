$version: "2.0"

namespace com.amazonaws.simple

@trait
@traitValidators(
    // Valid
    "foo.bar": {
        selector: "~> string"
        message: "This one is invalid"
    }
    // Valid
    "foo_bar": {
        selector: "~> string"
        message: "This one is invalid"
    }
    // Valid
    "_foo_bar": {
        selector: "~> string"
        message: "This one is invalid"
    }
    // Valid
    "Foo.bar": {
        selector: "~> string"
        message: "This one is invalid"
    }
    // Valid
    "Foo": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure allValid {}


@trait
@traitValidators(
    // Invalid
    ".": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid1 {}

@trait
@traitValidators(
    // Invalid
    "foo.": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid2 {}

@trait
@traitValidators(
    // Invalid
    "_": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid3 {}

@trait
@traitValidators(
    // Invalid
    "1": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid4 {}

@trait
@traitValidators(
    // Invalid
    "a.1": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid5 {}

@trait
@traitValidators(
    // Invalid
    ".a": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid6 {}

@trait
@traitValidators(
    // Invalid
    "!.a": {
        selector: "~> string"
        message: "This one is invalid"
    }
)
structure invalid7 {}
