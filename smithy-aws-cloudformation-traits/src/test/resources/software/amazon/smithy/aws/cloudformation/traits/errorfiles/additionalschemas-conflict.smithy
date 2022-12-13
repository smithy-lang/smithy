$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource

service AdditionalSchemasConflict {
    version: "2020-07-02",
    resources: [
        AdditionalSchemasConflictResource,
    ],
}

@cfnResource(additionalSchemas: [AdditionalSchemasConflictProperties])
resource AdditionalSchemasConflictResource {
    identifiers: {
        fooId: String,
    },
    create: CreateAdditionalSchemasConflictResource,
}

operation CreateAdditionalSchemasConflictResource {
    input: CreateAdditionalSchemasConflictResourceRequest,
    output: CreateAdditionalSchemasConflictResourceResponse
}

@input
structure CreateAdditionalSchemasConflictResourceRequest {
    bar: String,
}

structure AdditionalSchemasConflictProperties {
    bar: Boolean,
}

@output
structure CreateAdditionalSchemasConflictResourceResponse {}
