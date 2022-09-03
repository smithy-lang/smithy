// {"v1-box": false, "v1-client-zero-value": false, "v2": false}
$version: "1.0"
namespace smithy.example

structure Foo {
    @required
    nonNullableIntegerUnboxedCustomTarget: MyPrimitiveInteger
}

integer MyPrimitiveInteger
