$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Widget {
    identifiers: { widgetId: String }
}

// The identifier path is not valid JMESPath.
@updatesResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId[" } }
    }
])
operation UpdateWidget {
    input: UpdateWidgetInput
    output: UpdateWidgetOutput
}

@input
structure UpdateWidgetInput {
    widgetId: String
}

structure UpdateWidgetOutput {}
