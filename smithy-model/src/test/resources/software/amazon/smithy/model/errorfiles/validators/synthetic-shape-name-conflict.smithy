$version: "2.1"

namespace smithy.example

// User-defined shape with reserved _Synthetic prefix should produce a warning.
string _SyntheticFoo

// Inline collections generate synthetic shapes and members whose names carry the
// reserved prefix. The generated shapes carry the synthetic trait and members are
// not user-named, so neither must warn.
structure MyStructure {
    strings: [String]
    tags: {String: String}
}
