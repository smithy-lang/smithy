$version: "2"

namespace com.example

@createsResources([
    {
        resource: "com.example#DoesNotExist"
        identifiers: { id: { path: "result.id" } }
    }
])
operation CreateWidget {
    input: CreateWidgetInput
    output: CreateWidgetOutput
}

@input
structure CreateWidgetInput {}

structure CreateWidgetOutput {
    result: ResultData
}

structure ResultData {
    id: String
}
