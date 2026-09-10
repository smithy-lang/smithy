$version: "2"

namespace smithy.benchmark.serde

resource DynamoDBItem {
    operations: [
        PutItem
        GetItem
    ]
}
