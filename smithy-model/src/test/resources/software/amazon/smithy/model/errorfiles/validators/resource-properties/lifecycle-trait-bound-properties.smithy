$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "smithy.example" }
]

namespace smithy.example

// Each property except `owner` is located only by a resource lifecycle trait binding on an
// operation that is not bound to Widget, so only `owner` is reported as unused.
resource Widget {
    identifiers: { widgetId: WidgetId }
    properties: {
        color: String
        size: Integer
        label: String
        createdAt: Timestamp
        status: String
        owner: String
    }
}

string WidgetId

// Explicit property on the input side.
@createsResources([
    {
        resource: Widget
        properties: { color: { path: "color" } }
    }
])
operation MakeWidget {
    input := {
        color: String
    }
}

// Explicit property on the input side.
@putsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        properties: { size: { path: "size" } }
    }
])
@idempotent
operation PutWidget {
    input := {
        @required
        widgetId: WidgetId

        size: Integer
    }
}

// Explicit property on the input side.
@updatesResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        properties: { label: { path: "label" } }
    }
])
operation RelabelWidget {
    input := {
        @required
        widgetId: WidgetId

        label: String
    }
}

// Explicit property nested in the output.
@readonly
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        properties: { createdAt: { path: "widget.createdAt" } }
    }
])
operation DescribeWidget {
    input := {
        @required
        widgetId: WidgetId
    }
    output := {
        widget: WidgetDescription
    }
}

structure WidgetDescription {
    createdAt: Timestamp
}

// Inferred property in the output.
@readonly
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        propertiesFrom: "state"
    }
])
operation GetWidgetStatus {
    input := {
        @required
        widgetId: WidgetId
    }
    output := {
        state: WidgetState
    }
}

structure WidgetState {
    status: String
}
