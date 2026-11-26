$version: "2"

namespace smithy.example

service TestService {
    resources: [
        Widget
    ]
}

// The property's link id matches the id of the resource's operation listing.
resource Widget {
    identifiers: {
        widgetId: String
    }
    properties: {
        operations: String
    }
    read: GetWidget
    operations: [
        PingWidget
    ]
}

@readonly
operation GetWidget {
    input := for Widget {
        @required
        $widgetId
    }
    output := for Widget {
        @required
        $widgetId

        $operations
    }
}

operation PingWidget {
    input := for Widget {
        @required
        $widgetId
    }
}
