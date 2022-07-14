// Mixins may only be mixed into shapes of the same type. The following mixins were applied to the structure shape `com.foo#Foo` which are not structure shapes: [`com.foo#Bar`]
$version: "2"
namespace com.foo

@mixin
union Bar {
    first: String
}

structure Foo with [Bar] {}
