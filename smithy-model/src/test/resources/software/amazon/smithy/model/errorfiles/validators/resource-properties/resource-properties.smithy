$version: "2.0"

namespace smithy.example

/// Serves as a somewhat complete and valid example of a
/// service model with resource defined properties.
service BookStore {
    version: "2022-04-01",
    resources: [
        Book
    ]
    operations: [TagResource, UntagResource]
}

resource Book {
    identifiers: {
        bookId: ISBN
    }
    properties: {
        title: Title
        publishDate: Date
        authors: AuthorList
        price: Price
        averageReviewScore: Float
        tagsSillyName: TagList
    }
    create: AddBook
    read: DescribeBook
    update: UpdateBook
    delete: DeleteBook
    list: ListBooks
    collectionOperations: [ListBooks]
}

structure BookDescription {
    title: Title
    publishDate: Date
    authors: AuthorList
    price: Price
}

structure Author {
    givenName: GivenName
    familyName: FamilyName
}

list AuthorList {
    member: Author
}

operation AddBook {
    input: AddBookRequest
    output: AddBookResponse
}

structure AddBookRequest {
    @required
    title: Title

    @required
    publishDate: Date

    @required
    authors: AuthorList

    @idempotencyToken
    @notProperty
    token: String

    @property(name: "tagsSillyName")
    tags: TagList
}

structure AddBookResponse {
    @required
    @resourceIdentifier("bookId")
    isbn: ISBN
}

@idempotent
operation DeleteBook {
    input: DeleteBookRequest
    output: DeleteBookResponse
}

structure DeleteBookRequest {
    @required
    @resourceIdentifier("bookId")
    isbn: ISBN
}

structure DeleteBookResponse { }

operation UpdateBook {
    input: UpdateBookRequest
    output: UpdateBookResponse
}

structure UpdateBookRequest {
    @required
    @resourceIdentifier("bookId")
    isbn: ISBN

    price: Price
    @property(name: "averageReviewScore")
    rating: Float

    @notProperty
    inventoryRefreshDate: Date

    @idempotencyToken
    @notProperty
    token: String

    @property(name: "tagsSillyName")
    tagReplace: TagList
}

structure UpdateBookResponse { }

@readonly
operation DescribeBook {
    input: DescribeBookRequest
    output: DescribeBookResponse
}

structure DescribeBookRequest {
    @required
    @resourceIdentifier("bookId")
    isbn: ISBN
}

structure DescribeBookResponse {
    @nestedProperties
    book: BookDescription
}

@readonly
@paginated(inputToken: "nextToken", outputToken: "nextToken", items: "books")
operation ListBooks {
    input: ListBooksRequest
    output: ListBooksResponse
}

structure ListBooksRequest {
    maxResults: Integer
    nextToken: String
}

structure ListBooksResponse {
    books: BookList
    nextToken: String
}

operation TagResource {
    input: TagResourceRequest
    output: TagResourceResponse
}

structure TagResourceRequest {
    @property(name:"tagsSillyName")
    tags: TagList
}

structure TagResourceResponse { }

operation UntagResource {
    input: UntagResourceRequest
    output: UntagResourceResponse
}

structure UntagResourceRequest {
    tagKeys: TagKeyList
}

structure UntagResourceResponse { }

@pattern("^\\d{13}$")
string ISBN

list BookList {
    member: BookDescription
}

structure Tag {
    key: String,
    value: String
}

list TagList {
    member: Tag
}

list TagKeyList {
    @length(min: 1, max:128)
    member: String
}

@pattern("^\\$\\d+(\\.\\d{2})?$")
string Price
string Title
string GivenName
string FamilyName
timestamp Date

