$version: "2.0"

namespace test.smithy.traitcodegen.mixins

// The following trait checks that mixins are correctly flattened by
// the trait codegen plugin
@trait
structure structWithMixin with [extras] {}

@private
@mixin
structure extras {
    @required
    d: String
}
