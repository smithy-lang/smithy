$version: "2"

namespace smithy.example

use smithy.contracts#conditions
use smithy.contracts#contracts

service LogFetcher {
    operations: [FetchLogs]
}

@contracts({
    StartBeforeEnd: {
        documentation: "The requested start time must be strictly less than the end time",
        expression: "input.start < input.end"
    }
})
operation FetchLogs {
    input: FetchLogsInput
}

@conditions({
    StartBeforeEnd: {
        documentation: "The start time must be strictly less than the end time",
        expression: "start < end"
    }
})
structure FetchLogsInput {
    @required
    start: Timestamp

    @required
    end: Timestamp
}

@conditions({
    NoKeywords: {
        documentation: "The name cannot contain either 'id' or 'name', as these are reserved keywords"
        expression: "!contains(@, 'id') && !contains(@, 'name')"
    }
})
string Name

// A deeper contract: a precondition that reads resource state from the `before`
// snapshot through a projected reference handle, rather than comparing two input
// members. The handle `input.document` is projected from the @references entry.
service DocumentService {
    operations: [PublishDocument]
    resources: [Document]
}

resource Document {
    identifiers: {documentId: String}
    properties: {status: String}
    read: GetDocument
}

@readonly
operation GetDocument {
    input: GetDocumentInput
    output: GetDocumentOutput
}

@input
structure GetDocumentInput {
    @required
    documentId: String
}

@output
structure GetDocumentOutput {
    status: String
}

@contracts({
    DocumentApprovedOnSuccess: {
        documentation: "A successful publish requires the referenced document to be approved beforehand"
        expression: "requires(@, resource(before, input.document).properties.status == 'APPROVED')"
    }
})
operation PublishDocument {
    input: PublishDocumentInput
    output: PublishDocumentOutput
}

@input
@references([
    {resource: Document, name: "document", ids: {documentId: "documentId"}}
])
structure PublishDocumentInput {
    @required
    documentId: String
}

@output
structure PublishDocumentOutput {}
