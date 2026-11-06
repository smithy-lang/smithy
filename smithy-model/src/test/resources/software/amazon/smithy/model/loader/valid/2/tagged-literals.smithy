// A version that omits the minor version resolves to the latest known minor
// version, so "2" grants the 2.1-only tagged string literals.
$version: "2"

namespace smithy.example

@pattern(#re "^\d{3}-\d{2}-\d{4}$")
string Ssn

structure TaggedLiteralsExample {
    @default(#timestamp "2024-01-01T00:00:00Z")
    startDate: Timestamp

    @default(#hex "89504e47")
    header: Blob

    @default(#b ".PNG\x0D\x0A\x1A\x0A")
    avatar: Blob
}
