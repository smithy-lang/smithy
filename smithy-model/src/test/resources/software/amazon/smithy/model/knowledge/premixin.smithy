$version: "2.0"

namespace smithy.example

@private
service MixedService with MixinService {
    version: "2022-01-01"
    operations: [
        MixedServiceOperation
    ]
    resources: [
        MixedResource
    ]
    errors: [
        MixedError
    ]
    rename: {
        "smithy.example#MixedRename": "LocalRename"
    }
}

@internal
@mixin
service MixinService {
    version: "2021-12-31"
    operations: [
        MixinServiceOperation
    ]
    resources: [
        MixinResource
    ]
    errors: [
        MixinError
    ]
    rename: {
        "smithy.example#MixinRename": "UpstreamRename"
    }
}

resource MixedResource {
}

resource MixinResource {
}

@mixin
@internal
operation MixinOperation {
    errors: [MixinError]
}

@private
operation MixedOperation with MixinOperation {
    errors: [MixedError]
}

operation MixedServiceOperation {
    input: Unit
    output: Unit
}

operation MixinServiceOperation {
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

@mixin
@internal
string MixinString

@private
string MixedString with MixinString

string OverriddenRename
