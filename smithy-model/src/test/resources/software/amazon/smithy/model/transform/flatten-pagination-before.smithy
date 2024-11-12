$version: "2.0"

namespace smithy.example

@paginated(inputToken: "nextToken", outputToken: "nextToken")
service PaginatedService {
    operations: [
        PaginatedOperation
    ]
}

@paginated(pageSize: "maxResults", items: "foos")
operation PaginatedOperation {
    input := {
        maxResults: Integer
        nextToken: String
    }
    output := {
        nextToken: String

        @required
        foos: StringList
    }
}

@private
list StringList {
    member: String
}
