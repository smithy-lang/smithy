$version: "2.1"

namespace smithy.example

use foo.example#Widget

// Inline collections resolve targets through use statements.
// `[Widget]` resolves to `foo.example#Widget` via the use import.
structure MyStructure {
    widgets: [Widget]
}
