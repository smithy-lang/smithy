// Operation shapes with the mixin trait MUST target `smithy.api#Unit` for their input and output. Operation mixin shape `smithy.example#MixinOperation` defines one or both of these properties.
$version: "2.0"

namespace smithy.example

@mixin
operation MixinOperation {
    input := {}
    output := {}
}
