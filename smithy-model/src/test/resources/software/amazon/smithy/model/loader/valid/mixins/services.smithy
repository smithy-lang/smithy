$version: "2.0"

namespace smithy.example

string ShapeToRename

@error("client")
structure MixinError {
    message: ShapeToRename
}

operation MixinOperation {}

resource MixinResource {}

@internal
@mixin
service MixinService {
    version: "2021-12-31"
    errors: [MixinError]
    rename: {
        "smithy.example#ShapeToRename": "RenamedShape"
    }
    operations: [MixinOperation]
    resources: [MixinResource]
}

service MixedService with [MixinService] {}
