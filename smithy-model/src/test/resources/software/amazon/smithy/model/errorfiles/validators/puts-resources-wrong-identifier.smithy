$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Table {
    identifiers: { tableName: String }
}

// `tableId` is not an identifier of Table (the identifier is `tableName`).
@putsResources([
    {
        resource: Table
        identifiers: { tableId: { path: "tableId" } }
    }
])
operation PutTable {
    input: PutTableInput
    output: PutTableOutput
}

@input
structure PutTableInput {
    tableId: String
}

structure PutTableOutput {}
