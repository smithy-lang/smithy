$version: "2"

namespace smithy.example

service TestService {
    operations: [
        NoSnippets
        BasicOperation
        PaginatedOperation
    ]
}

@examples([
    {
        title: "Basic Example"
        input: {
            foo: "foo"
        }
        output: {
            bar: "bar"
        }
    }
    {
        title: "Error Example"
        input: {
            foo: "bar"
        }
        error: {
            shapeId: BasicError
            content: {
                message: "bar"
            }
        }
    }
])
operation BasicOperation {
    input := {
        foo: String
    }
    output := {
        bar: String
    }
    errors: [
        BasicError
    ]
}

@examples([
    {
        title: "Basic Example"
        input: {
            foo: "foo"
        }
        output: {
            bar: "bar"
        }
    }
    {
        title: "Error Example"
        input: {
            foo: "bar"
        }
        error: {
            shapeId: BasicError
            content: {
                message: "bar"
            }
        }
    }
])
operation NoSnippets {
    input := {
        foo: String
    }
    output := {
        bar: String
    }
    errors: [
        BasicError
    ]
}

@error("client")
structure BasicError {
    message: String
}

@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "pageSize", items: "items")
operation PaginatedOperation {
    input: PaginatedOperationInput

    output := {
        items: Items

        nextToken: String
    }
}

@input
structure PaginatedOperationInput {
    nextToken: String

    pageSize: Integer
}

list Items {
    member: String
}
