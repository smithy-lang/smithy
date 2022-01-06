$version: "1.0"

namespace smithy.example

use aws.cloudformation#cfnResource

service AdditionalSchemasNotBound {
    version: "2020-07-02",
    resources: [AdditionalSchemasNotBoundResource],
}

@cfnResource(additionalSchemas: [AdditionalSchemasNotBoundProperties])
resource AdditionalSchemasNotBoundResource {
    identifiers: {
        fooId: String,
    },
    create: CreateAdditionalSchemasNotBoundResource,
}

operation CreateAdditionalSchemasNotBoundResource {
    input: CreateAdditionalSchemasNotBoundResourceRequest,
    output: CreateAdditionalSchemasNotBoundResourceResponse
}

@input
structure CreateAdditionalSchemasNotBoundResourceRequest {
    baz: String,
}

structure AdditionalSchemasNotBoundProperties {
    bar: Boolean,
}

@output
structure CreateAdditionalSchemasNotBoundResourceResponse {}

service AdditionalSchemasBound {
    version: "2020-07-02",
    resources: [AdditionalSchemasNotBoundResource],
    shapes: [AdditionalSchemasNotBoundProperties],
}
