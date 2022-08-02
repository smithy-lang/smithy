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
    update: UpdateResource
    delete: DeleteResource
    operations: [SetTitle]
}

operation SetTitle {
    input := {
        @required
        @resourceIdentifier("productId")
        id: Identifier
        @nestedProperties
        title: Title
    }
    output := {}
}

structure ResourceDescription {
    title: Title
    publishDate: Date
    price: Price
    @notProperty
    invalidToken: String
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

operation UpdateResource {
    input: UpdateResourceRequest
    output: UpdateResourceResponse
}

structure UpdateBookNest {
    /// Following will result in a double nesting (attempt).
    @nestedProperties
    book: ResourceDescription
}

structure UpdateResourceRequest {
    @required
    @resourceIdentifier("productId")
    id: Identifier
    @nestedProperties
    book: UpdateBookNest 
    @idempotencyToken
    token: String
}

structure UpdateResourceResponse {
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

