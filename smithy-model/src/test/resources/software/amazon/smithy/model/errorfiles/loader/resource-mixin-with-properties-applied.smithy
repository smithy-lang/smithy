$version: "2.0"

namespace smithy.example

resource AppliedMixinResource {
    operations: [Op]
}

operation Op {}

apply AppliedMixinResource @mixin
