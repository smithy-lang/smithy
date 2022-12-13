// Parse error at line 7, column 5 near `with`: Expected: '{', but found 'w'
$version: "2.0"

namespace smithy.example

service MixedService
    with [MixinService] {}

@mixin
service MixinService {}
