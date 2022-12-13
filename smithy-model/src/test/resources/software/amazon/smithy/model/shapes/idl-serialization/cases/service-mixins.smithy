$version: "2.0"

namespace smithy.example

service MixedService with [MixinService] {
}

@internal
@mixin
service MixinService {
    version: "2021-12-31"
    operations: [
        MixinOperation
    ]
    resources: [
        MixinResource
    ]
    errors: [
        MixinError
    ]
    rename: {
        "smithy.example#ShapeToRename": "RenamedShape"
    }
}

resource MixinResource {
}

operation MixinOperation {
    input: Unit
    output: Unit
}

@error("client")
structure MixinError {
    message: ShapeToRename
}

string ShapeToRename
