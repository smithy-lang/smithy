$version: "2.1"

namespace smithy.example

resource Widget {
    identifiers: {
        widgetId: String
    }
    properties: {
        name: String
    }
    read: GetWidget
}

@readonly
operation GetWidget {
    input := {
        @required
        widgetId: String
    }
    output := {
        name: String
    }
}
