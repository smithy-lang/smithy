$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource
use aws.cloudformation#cfnExcludeProperty

service AdditionalSchemasDeconflicted {
    version: "2020-07-02",
    resources: [
        AdditionalSchemasDeconflictedResource,
    ],
}

@cfnResource(additionalSchemas: [AdditionalSchemasDeconflictedProperties])
resource AdditionalSchemasDeconflictedResource {
    identifiers: {
        fooId: String,
    },
    create: CreateAdditionalSchemasDeconflictedResource,
}

operation CreateAdditionalSchemasDeconflictedResource {
    input: CreateAdditionalSchemasDeconflictedResourceRequest,
    output: CreateAdditionalSchemasDeconflictedResourceResponse
}

@input
structure CreateAdditionalSchemasDeconflictedResourceRequest {
    @cfnExcludeProperty
    bar: String,
}

structure AdditionalSchemasDeconflictedProperties {
    bar: Boolean,
}

@output
structure CreateAdditionalSchemasDeconflictedResourceResponse {}
