$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Item {
    identifiers: { itemId: String }
}

@createsResources([
    {
        resource: "com.example#Item"
        identifiers: { itemId: { path: "length(items)" } }
    }
])
operation CreateItems {
    input: CreateItemsInput
    output: CreateItemsOutput
}

@input
structure CreateItemsInput {}

structure CreateItemsOutput {
    items: ItemList
}

list ItemList {
    member: String
}
