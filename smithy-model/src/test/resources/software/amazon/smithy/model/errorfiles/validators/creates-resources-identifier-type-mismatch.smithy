$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

// The resource identifier targets WidgetId (a constrained string). The binding locates it at a
// member that targets plain String, which no longer matches now that identifiers are checked
// against the resource identifier's exact target shape.
resource Widget {
    identifiers: { widgetId: WidgetId }
}

@length(min: 1, max: 128)
string WidgetId

@createsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
    }
])
operation CreateWidget {
    input: CreateWidgetInput
    output: CreateWidgetOutput
}

@input
structure CreateWidgetInput {}

structure CreateWidgetOutput {
    widgetId: String
}
