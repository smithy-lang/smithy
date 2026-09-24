$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Table {
    identifiers: { tableName: String }
}

@putsResources([
    {
        resource: "com.example#Table"
        identifiers: { tableName: { path: "tableName" } }
    }
])
operation CreateTable {
    input: CreateTableInput
    output: CreateTableOutput
}

@input
structure CreateTableInput {
    tableName: String
}

structure CreateTableOutput {}
