$version: "2.1"

namespace smithy.example

@pattern(#re "^\d{3}-\d{2}-\d{4}$")
string SSNFormat

@pattern(#re "^\d+$")
string DigitsOnly
