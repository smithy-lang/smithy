// Parse error at line 7, column 1 near `use`: A use statement must come before any shape definition
namespace smithy.example

@trait
structure foo {}

use smithy.api#String
