// Cannot `use shape` name `foo.baz#String` because it conflicts with `smithy.api#String`
$version: "0.2.0"

use shape smithy.api#String
use shape foo.baz#String
