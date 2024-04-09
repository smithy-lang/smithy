// Constraints are not fully-transitive. They work only one level deep.
$version: "2.0"

namespace com.amazonaws.simple

@trait
@traitValidators(
    "NoSensitiveStrings": {
        selector: "~> member :test(> [trait|sensitive])"
        message: "Sensitive strings are not allowed here"
    }
)
structure noSensitive {}

@trait
@noSensitive
structure transitiveTrait {}

@transitiveTrait
list MyStrings {
    member: SensitiveString
}

@sensitive
string SensitiveString
