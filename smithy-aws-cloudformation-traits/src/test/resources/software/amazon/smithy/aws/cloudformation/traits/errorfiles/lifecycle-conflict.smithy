$version: "1.0"

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
}

structure CreateLifecycleConflictResourceRequest {
    bar: String,
}

@readonly
operation GetLifecycleConflictResource {
    input: GetLifecycleConflictResourceRequest,
    output: GetLifecycleConflictResourceResponse,
}

structure GetLifecycleConflictResourceRequest {
    @required
    fooId: String,
}

structure GetLifecycleConflictResourceResponse {
    bar: Boolean,
}

