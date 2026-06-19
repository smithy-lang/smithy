// One-of-N policy for MemberShouldReferenceResource: when a member's name and
// target match multiple resources, declaring a `@references` trait for at least
// one of them silences the warning for all of them. The warning only fires when
// the structure references none of the matching resources.
$version: "2.0"

namespace ns.multi

string WidgetId
string ContextId

resource Widget {
    identifiers: {
        widgetId: WidgetId
    }
}

resource WidgetContext {
    identifiers: {
        widgetId: WidgetId
        contextId: ContextId
    }
}

// Partial coverage: matches both Widget and WidgetContext; @references lists Widget only.
// One-of-N silences the warning.
@references([{resource: Widget}])
structure PartialCoverageInput {
    widgetId: WidgetId
}

// Zero coverage: matches both resources, no @references trait.
// Warning fires and lists every match.
structure ZeroCoverageInput {
    widgetId: WidgetId
}

// Suppression on the member still silences the zero-coverage warning.
structure SuppressedInput {
    @suppress(["MemberShouldReferenceResource"])
    widgetId: WidgetId
}
