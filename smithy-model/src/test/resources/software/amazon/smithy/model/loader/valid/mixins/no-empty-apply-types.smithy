$version: "2.0"
namespace smithy.example

@mixin
structure MixinStruct {
    bar: PrimitiveInteger = null
}

structure MixedStruct with [MixinStruct] {}
