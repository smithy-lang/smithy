$version: "2.0"

namespace smithy.example

@trait(
    breakingChanges: [
        {
            path: "",
            change: "presence",
            message: "Add or remove this this trait, and it is a backward incompatible change!"
        }
    ]
)
string someTrait
