$version: "2"

metadata suppressions = [
    { id: "UnstableTrait", namespace: "com.example" }
]

namespace com.example

resource Gadget {
    identifiers: { gadgetId: GadgetId }
}

string GadgetId

// A create-only put with a caller-chosen identifier nested in a wrapper.
@putsResources([
    {
        resource: Gadget
        identifiers: { gadgetId: { path: "gadget.gadgetId" } }
        noReplace: true
    }
])
@idempotent
operation CreateGadget {
    input := {
        @required
        gadget: GadgetSpec
    }
}

structure GadgetSpec {
    @required
    gadgetId: GadgetId
}

// noReplace only applies to puts.
@createsResources([
    {
        resource: Gadget
        noReplace: true
    }
])
operation MakeGadget {}

// A redundant binding on the resource's own put: consumers of the standard put read the resource's
// @noReplace trait, so the warning points there.
resource Widget {
    identifiers: { widgetId: WidgetId }
    put: PutWidget
}

string WidgetId

@putsResources([
    {
        resource: Widget
        noReplace: true
    }
])
@idempotent
operation PutWidget {
    input := {
        @required
        widgetId: WidgetId
    }
}

