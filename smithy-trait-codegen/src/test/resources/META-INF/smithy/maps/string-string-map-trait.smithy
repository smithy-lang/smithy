$version: "2.0"

namespace test.smithy.traitcodegen.maps

/// Map of only simple strings. These are handled slightly differently than
/// other maps
@trait
map StringStringMap {
    key: String
    value: String
}
