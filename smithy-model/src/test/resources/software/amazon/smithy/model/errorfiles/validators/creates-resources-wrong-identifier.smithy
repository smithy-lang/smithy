$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: String }
}

@createsResources([
    {
        resource: "com.example#Widget"
        identifiers: { wrongName: { path: "result.id" } }
    }
])
operation CreateWidget {
    input: CreateWidgetInput
    output: CreateWidgetOutput
}

@input
structure CreateWidgetInput {}

structure CreateWidgetOutput {
    result: WidgetResult
}

structure WidgetResult {
    id: String
}
