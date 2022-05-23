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
        bookId: ISBN
    }
    properties: {
        title: Title
        price: Price
    }
    create: AddResource
    delete: DeleteResource
}

operation AddResource {
    input: AddResourceRequest
    output: AddResourceResponse
}

structure AddResourceRequest {
    @required
    title: Title

    // missing price property
    @idempotencyToken
    @notProperty
    token: String
}

structure AddResourceResponse {
    @required
    @resourceIdentifier("bookId")
    isbn: ISBN
}

@idempotent
operation DeleteResource {
    input: DeleteResourceRequest
    output: DeleteResourceResponse
}

structure DeleteResourceRequest {
    @required
    bookId: ISBN
}

structure DeleteResourceResponse { }

@pattern("^\\d{13}$")
string ISBN

@pattern("^\\$\\d+(\\.\\d{2})?$")
string Price
string Title

