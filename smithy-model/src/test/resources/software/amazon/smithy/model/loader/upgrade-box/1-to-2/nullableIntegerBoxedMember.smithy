// {"v1-box": true, "v1-client-zero-value": true, "v2": true}
$version: "1.0"
namespace smithy.example

structure Foo {
    @box
    nullableIntegerBoxedMember: MyPrimitiveInteger
}

integer MyPrimitiveInteger
