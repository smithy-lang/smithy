$version: "2.0"

namespace smithy.example

use smithy.api#length // def
use smithy.api#sensitive // abc

// Comment
/// A
@sensitive
@length(min: 1)
string A
