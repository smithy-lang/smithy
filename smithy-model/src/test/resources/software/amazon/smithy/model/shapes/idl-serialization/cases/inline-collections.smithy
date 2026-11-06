$version: "2"

namespace smithy.example

structure MyStructure {
    strings: [String]
    tags: {String: String}
    nested: {String: [Integer]}
}
