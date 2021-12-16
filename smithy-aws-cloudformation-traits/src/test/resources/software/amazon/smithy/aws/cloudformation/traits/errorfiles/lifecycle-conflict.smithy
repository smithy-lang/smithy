$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource

service LifecycleConflict {
    version: "2020-07-02",
    resources: [
        LifecycleConflictResource,
    ],
}

@cfnResource
resource LifecycleConflictResource {
    identifiers: {
        fooId: String,
    },
    create: CreateLifecycleConflictResource,
    read: GetLifecycleConflictResource,
}

operation CreateLifecycleConflictResource {
    input: CreateLifecycleConflictResourceRequest,
    output: CreateLifecycleConflictResourceResponse
}

@input
structure CreateLifecycleConflictResourceRequest {
    bar: String,
}

@output
structure CreateLifecycleConflictResourceResponse {}

@readonly
operation GetLifecycleConflictResource {
    input: GetLifecycleConflictResourceRequest,
    output: GetLifecycleConflictResourceResponse,
}

@input
structure GetLifecycleConflictResourceRequest {
    @required
    fooId: String,
}

@output
structure GetLifecycleConflictResourceResponse {
    bar: Boolean,
}

