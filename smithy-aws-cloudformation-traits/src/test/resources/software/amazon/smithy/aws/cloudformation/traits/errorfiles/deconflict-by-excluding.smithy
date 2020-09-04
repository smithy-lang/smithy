$version: "1.0"

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
}

structure CreateAdditionalSchemasDeconflictedResourceRequest {
    @cfnExcludeProperty
    bar: String,
}

structure AdditionalSchemasDeconflictedProperties {
    bar: Boolean,
}
