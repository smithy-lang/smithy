$version: "2.0"
namespace smithy.example

@mixin
@trait
structure mixinTrait {}

// This is fine, and it makes this shape a trait!
structure usesMixinTrait with [mixinTrait] {}

@usesMixinTrait
structure FineUseOfTrait {}

@mixinTrait // cannot apply a trait with a mixin as a trait.
structure InvalidUseOfMixinTrait {}
