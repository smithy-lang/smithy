namespace smithy.example

use aws.cloudformation#cfnMutability
use aws.cloudformation#cfnResource

service TestService {
    version: "2020-07-02",
    resources: [RequiredOutput]
}

@cfnResource
resource RequiredOutput {
    identifiers: {
        fooId: String,
    },
    create: CreateFoo,
    read: ReadFoo,
}

operation CreateFoo {
    input: CreateFooRequest,
    output: CreateFooResponse,
}

structure CreateFooRequest {
    @required
    tags: TagList,
}

structure CreateFooResponse {
    @required
    fooId: String,
}

@readonly
operation ReadFoo {
    input: ReadFooRequest,
    output: ReadFooResponse,
}

structure ReadFooRequest {
    @required
    fooId: String,
}

@output
structure ReadFooResponse {
    @required
    fooId: String,

    // This property should be included as required since is also part
    // of the request as @required
    @required
    tags: TagList,

    // This property should NOT be included as required since is only
    // part of an output structure.
    @required
    lastUpdate: Timestamp,
}

list TagList {
    member: String
}
