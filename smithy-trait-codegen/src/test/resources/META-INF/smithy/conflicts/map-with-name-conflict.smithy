$version: "2.0"

namespace test.smithy.traitcodegen.conflicts

use test.smithy.traitcodegen.conflicts#MapOfMap

@trait
map MapWithNameConflict {
    key: String
    value: MapOfMap
}
