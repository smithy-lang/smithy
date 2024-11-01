$version: "2.0"

namespace ns.foo

use aws.api#arn
use aws.api#service

@service(sdkId: "Some Value")
service SomeService {
    version: "2018-03-17"
    resources: [
        Resource1
    ]
}

@arn(template: "foo")
resource Resource1 {
    resources: [
        Resource2
    ]
}

@arn(template: "foo/{id}")
resource Resource2 {
    identifiers: { id: String }
    operations: [
        InstanceOperation
    ]
    collectionOperations: [
        CollectionOperation
    ]
}

operation CollectionOperation {
    input: CollectionOperationInput
    output: Unit
}

operation InstanceOperation {
    input: InstanceOperationInput
    output: Unit
}

structure CollectionOperationInput {}

structure InstanceOperationInput {
    @required
    id: String
}
