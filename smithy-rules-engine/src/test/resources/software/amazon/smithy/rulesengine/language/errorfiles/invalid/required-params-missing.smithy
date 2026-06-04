$version: "2.0"

namespace example

use smithy.rules#clientContextParams
use smithy.rules#endpointRuleSet
use smithy.rules#staticContextParams

@endpointRuleSet({
    version: "1.3"
    parameters: {
        Region: { type: "string", required: true, documentation: "docs" }
        Bucket: { type: "string", required: true, documentation: "docs" }
    }
    rules: []
})
@clientContextParams(
    Region: { type: "string", documentation: "docs" }
)
service MyService {
    operations: [
        HasBucket
        MissingBucket
    ]
}

@staticContextParams(
    Bucket: { value: "my-bucket" }
)
operation HasBucket {
    input: HasBucketInput
}

structure HasBucketInput {}

operation MissingBucket {
    input: MissingBucketInput
}

structure MissingBucketInput {}
