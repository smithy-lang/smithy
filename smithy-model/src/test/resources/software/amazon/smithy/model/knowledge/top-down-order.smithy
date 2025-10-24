$version: "2.0"

namespace com.example


service Service {
    operations: [
        OperationB
        OperationA
        OperationC
    ]
    resources: [
        ResourceA
    ]
}

operation OperationB { }

operation OperationC {}

operation OperationA {}

resource ResourceA {
    operations: [
        OperationD
        OperationO
        OperationG
    ]
    resources: [
        ResourceC
        ResourceB
    ]
}

resource ResourceB {}

resource ResourceC {}

operation OperationD {}

operation OperationG {}

operation OperationO {}
