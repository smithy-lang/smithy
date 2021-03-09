$version: "1.0"

namespace aws.protocoltests.restjsonWithRenames

use aws.api#service
use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST JSON service that sends JSON requests and responses.
/// This test service is intended to test the `rename`
/// functionality of services and it effect on codegen.
@service(sdkId: "Rest Json Protocol")
@restJson1
service RestJsonWithRenames {
    version: "2019-12-16",
    rename: {
        "aws.protocoltests.restjsonWithRenames.nested#Foo": "NestedFoo",
    },
    operations: [
        SayHello,
    ]
}

operation SayHello {
    input: SayHelloInput,
}

structure SayHelloInput {
    foo: Foo,
    nestedFoo: aws.protocoltests.restjsonWithRenames.nested#Foo,
}

structure Foo {}
