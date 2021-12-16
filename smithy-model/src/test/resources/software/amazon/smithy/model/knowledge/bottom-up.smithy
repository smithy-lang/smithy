$version: "2"

namespace smithy.example

service Example {
    version: "2019-04-02",
    operations: [ServiceOperation],
    resources: [Resource1, Resource2]
}

operation ServiceOperation {}

resource Resource1 {
    operations: [Resource1Operation],
    resources: [Resource1_1, Resource1_2]
}

operation Resource1Operation {}

resource Resource1_2 {}

resource Resource1_1 {
    operations: [Resource1_1_Operation],
    resources: [Resource1_1_1, Resource1_1_2]
}

operation Resource1_1_Operation {}

resource Resource1_1_1 {
    operations: [Resource1_1_1_Operation],
}

operation Resource1_1_1_Operation {}

resource Resource1_1_2 {
    operations: [Resource1_1_2_Operation],
}

operation Resource1_1_2_Operation {}

resource Resource2 {}
