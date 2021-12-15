// Resource shapes with the mixin trait may not define any properties. Resource mixin shape `smithy.example#MixinResource` defines one or more properties.
$version: "2.0"

namespace smithy.example

@mixin
@internal
resource MixinResource {
    identifiers: {
        "mixinId": String
    }
}
