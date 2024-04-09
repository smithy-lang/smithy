// Use a 1.0 model to use an enum trait without a warning.
$version: "1.0"
namespace smithy.example

// This is used for assertions around de-duping files because duplicate enum values would fail validation.
@enum([
    {
        value: "FOO",
        name: "FOO"
    },
    {
        value: "BAR",
        name: "BAR"
    }
])
string MyString
