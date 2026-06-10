// Tagged string literals require Smithy IDL version 2.1 or later
$version: "2"

namespace smithy.example

@pattern(#re "^\d+$")
string Foo
