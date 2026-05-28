$version: "2.0"

metadata shapeClosures = [
    {
        id: "com.example#operations"
        includeBySelector: "operation"
    }
]

namespace com.example

service Weather {
    operations: [GetCity]
}

operation GetCity {
    input := {
        cityId: String
    }
}
