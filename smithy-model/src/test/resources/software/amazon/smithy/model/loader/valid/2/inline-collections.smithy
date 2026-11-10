// A version that omits the minor version resolves to the latest known minor
// version, so "2" grants the 2.1-only inline collection syntax.
$version: "2"

namespace smithy.example

structure MyStructure {
    strings: [String]
    tags: {String: String}
    nested: {String: [Integer]}
}
