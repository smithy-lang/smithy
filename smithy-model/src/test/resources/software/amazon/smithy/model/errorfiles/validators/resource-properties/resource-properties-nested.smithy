$version: "2.0"

namespace smithy.example

service ResourceStore {
    version: "2022-04-01",
    resources: [
        Resource
    ]
}

resource Resource {
    identifiers: {
        productId: Identifier
    }
    properties: {
        title: Title
        publishDate: Date
        price: Price
    }
    create: AddResource
    delete: DeleteResource
}

structure ResourceDescription {
    title: Title
    publishDate: Date
    price: Price
}

operation AddResource {
    input: AddResourceRequest
    output: AddResourceResponse
}

structure AddResourceRequest {
    @nestedProperties
    book: ResourceDescription
    @idempotencyToken
    token: String
}

structure AddResourceResponse {
    @required
    @resourceIdentifier("productId")
    id: Identifier
}

@idempotent
operation DeleteResource {
    input: DeleteResourceRequest
    output: DeleteResourceResponse
}

structure DeleteResourceRequest {
    @required
    @resourceIdentifier("productId")
    id: Identifier
}

structure DeleteResourceResponse { }

@pattern("^\\d{13}$")
string Identifier

@pattern("^\\$\\d+(\\.\\d{2})?$")
string Price
string Title
timestamp Date

