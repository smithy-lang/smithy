$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "smithy.example" }
]

namespace smithy.example

// Each property except `owner` and `weight` is located only by a resource lifecycle trait
// binding on an operation that is not bound to Widget. `owner` is located nowhere, and the only
// binding for `weight` has a path that resolves to nothing, so both are reported as unused.
resource Widget {
    identifiers: { widgetId: WidgetId }
    properties: {
        color: String
        size: Integer
        label: String
        createdAt: Timestamp
        status: String
        owner: String
        weight: Integer
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

// Explicit property whose path resolves to nothing, so it does not count as used.
@readonly
@readsResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
        properties: { weight: { path: "widget.weight" } }
    }
])
operation WeighWidget {
    input := {
        @required
        widgetId: WidgetId
    }
    output := {
        widget: WidgetDescription
    }
}

// A binding on the resource's own read operation is redundant, so it does not count: `nickname`
// is reported as unused, as it would be without the trait.
resource Gadget {
    identifiers: { gadgetId: GadgetId }
    properties: { nickname: String }
    read: GetGadget
}

string GadgetId

@readonly
@readsResources([
    {
        resource: Gadget
        identifiers: { gadgetId: { path: "gadgetId" } }
        properties: { nickname: { path: "gadget.nickname" } }
    }
])
operation GetGadget {
    input := {
        @required
        gadgetId: GadgetId
    }
    output := {
        @notProperty
        gadget: GadgetDescription
    }
}

structure GadgetDescription {
    nickname: String
}

// Delete bindings locate no properties; this checks they are skipped for a resource that has
// properties.
@deletesResources([
    {
        resource: Widget
        identifiers: { widgetId: { path: "widgetId" } }
    }
])
@idempotent
operation DiscardWidget {
    input := {
        @required
        widgetId: WidgetId
    }
}

// One operation binds both resources, but only its Widget binding locates `color`, so
// Sprocket's `color` is reported as unused.
resource Sprocket {
    identifiers: { sprocketId: SprocketId }
    properties: { color: String }
}

string SprocketId

@createsResources([
    {
        resource: Widget
        properties: { color: { path: "color" } }
    }
    {
        resource: Sprocket
    }
])
operation MakeWidgetAndSprocket {
    input := {
        color: String
    }
}
