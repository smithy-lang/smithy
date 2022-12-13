// smithy.example#ResourceStruct$id: Member target was elided, but no bound resource or mixin contained a matching identifier or member name.
$version: "2.0"

namespace smithy.example

resource MyResource {}

structure ResourceStruct for MyResource {
    $id
}
