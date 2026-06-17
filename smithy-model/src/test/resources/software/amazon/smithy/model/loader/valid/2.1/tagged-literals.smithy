$version: "2.1"

namespace smithy.example

@pattern(#re "^\d{3}-\d{2}-\d{4}$")
string Ssn

@pattern(#re "\w+")
string Word

@pattern(#re "a\"b")
string QuoteInRegex

structure TaggedLiteralsExample {
    @default(#timestamp "2024-01-01T00:00:00Z")
    startDate: Timestamp

    @default(#b ".PNG\x0D\x0A\x1A\x0A")
    avatar: Blob

    @default(#b "a\"b")
    quoted: Blob

    @default(#hex "89504e47")
    header: Blob
}
