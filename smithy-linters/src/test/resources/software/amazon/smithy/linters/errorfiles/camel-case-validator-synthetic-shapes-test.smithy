$version: "2.1"

metadata validators = [
        {name: "CamelCase",
        id: "DefaultCamelCase"}
]

namespace smithy.example

// Inline collections generate synthetic shapes whose names use a reserved
// `_Synthetic` prefix. Those generated shapes are not subject to camel case
// naming, so the CamelCase validator must not flag them.
structure MyStructure {
    strings: [String]
    tags: {String: String}
    nested: {String: [Integer]}
}
