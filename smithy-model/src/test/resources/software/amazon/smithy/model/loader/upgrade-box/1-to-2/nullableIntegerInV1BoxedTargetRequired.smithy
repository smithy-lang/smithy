// {"v1-box": true, "v1-client-zero-value": true, "v2": false}
$version: "1.0"
namespace smithy.example

structure Foo {
    @required
    nullableIntegerInV1BoxedTargetRequired: Integer,
}
