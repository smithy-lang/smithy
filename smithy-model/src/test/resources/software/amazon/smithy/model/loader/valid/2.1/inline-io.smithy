$version: "2.1"

namespace smithy.example

operation GetWidget {
    input := {
        widgetId: String
    }
    output := {
        name: String
        description: String
    }
}
