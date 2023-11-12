$version: "2.0"

namespace smithy.example

use smithy.api#sensitive // abc
use smithy.api#length // def

// Comment
/// A
@sensitive
@length(min: 1)
string A
