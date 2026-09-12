$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
    { id: "MemberShouldReferenceResource", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: String }
}

@createsResources([
    {
        resource: "com.example#Widget"
        identifiers: { widgetId: { path: "result.nonExistentField" } }
    }
])
operation CreateWidget {
    input: CreateWidgetInput
    output: CreateWidgetOutput
}

@input
structure CreateWidgetInput {}

structure CreateWidgetOutput {
    result: ResultData
}

structure ResultData {
    widgetId: String
}
