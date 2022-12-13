$version: "2.0"

namespace smithy.example

service MixedService with [MixinService] {
    version: "2022-01-01"
    operations: [
        MixedOperation
    ]
    resources: [
        MixedResource
    ]
    errors: [
        MixedError
    ]
    rename: {
        "smithy.example#OverriddenRename": "Override"
        "smithy.example#MixedRename": "LocalRename"
    }
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
        "smithy.example#MixinRename": "ThisWillBeInherited"
        "smithy.example#OverriddenRename": "ThisWillBeOverridden"
    }
}

resource MixedResource {
}

resource MixinResource {
}

operation MixedOperation {
    input: Unit
    output: Unit
}

operation MixinOperation {
    input: Unit
    output: Unit
}

@error("server")
structure MixedError {
    message: MixedRename
}

@error("client")
structure MixinError {
    message: MixinRename
    state: OverriddenRename
}

string MixedRename

string MixinRename

string OverriddenRename
