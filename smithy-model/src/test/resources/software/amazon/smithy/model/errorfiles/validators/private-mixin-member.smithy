$version: "2.0"

namespace smithy.example

@mixin
structure MixinWithPrivateMember {
    @private
    sensitiveField: String
}

structure ConsumerSameNamespace with [MixinWithPrivateMember] {
    @required
    $sensitiveField
}
