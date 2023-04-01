$version: "2.0"

namespace smithy.example

use aws.cloudformation#cfnResource

service InvalidAdditionalSchemasShape {
    version: "2020-07-02",
    resources: [
        InvalidAdditionalSchemasShapeResource,
    ],
}

@cfnResource(
  additionalSchemas: [ListShape]
)
resource InvalidAdditionalSchemasShapeResource {
    identifiers: {
        fooId: String,
    },
    create: CreateInvalidAdditionalSchemasShapeResource,
    read: GetInvalidAdditionalSchemasShapeResource,
}

list ListShape {
    member: String
}

operation CreateInvalidAdditionalSchemasShapeResource {
    input: CreateInvalidAdditionalSchemasShapeResourceRequest,
    output: CreateInvalidAdditionalSchemasShapeResourceResponse
}

@input
structure CreateInvalidAdditionalSchemasShapeResourceRequest {
    bar: String,
}

@output
structure CreateInvalidAdditionalSchemasShapeResourceResponse {}

@readonly
operation GetInvalidAdditionalSchemasShapeResource {
    input: GetInvalidAdditionalSchemasShapeResourceRequest,
    output: GetInvalidAdditionalSchemasShapeResourceResponse,
}

@input
structure GetInvalidAdditionalSchemasShapeResourceRequest {
    @required
    fooId: String,
}

@output
structure GetInvalidAdditionalSchemasShapeResourceResponse {
    bar: String,
}

