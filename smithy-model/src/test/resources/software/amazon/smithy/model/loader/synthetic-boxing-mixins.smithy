$version: "2.0"
namespace smithy.example

@mixin
integer MixinInteger

integer MixedInteger with [MixinInteger]

@mixin
structure MixinStruct {
    bar: MixedInteger = null
}

structure MixedStruct with [MixinStruct] {}
