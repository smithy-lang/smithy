$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Table {
    identifiers: { tableName: String }
}

@readsResources([
    {
        resource: "com.example#Table"
        identifiers: { tableName: { path: "tableNames[*]" } }
    }
])
operation BatchGetItem {
    input: BatchGetItemInput
    output: BatchGetItemOutput
}

@updatesResources([
    {
        resource: "com.example#Table"
        identifiers: { tableName: { path: "tableNames[*]" } }
    }
])
operation BatchWriteItem {
    input: BatchWriteItemInput
    output: BatchWriteItemOutput
}

@input
structure BatchGetItemInput {
    tableNames: TableNameList
}

structure BatchGetItemOutput {}

@input
structure BatchWriteItemInput {
    tableNames: TableNameList
}

structure BatchWriteItemOutput {}

list TableNameList {
    member: String
}
