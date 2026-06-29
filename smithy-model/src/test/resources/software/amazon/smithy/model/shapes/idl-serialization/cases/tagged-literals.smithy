$version: "2.1"

namespace smithy.example

structure TaggedLiteralsRoundTrip {
    avatar: Blob = "LlBORw0KGgo="
    startDate: Timestamp = 1704067200
}

@pattern("^\\d{3}-\\d{2}-\\d{4}$")
string Ssn
