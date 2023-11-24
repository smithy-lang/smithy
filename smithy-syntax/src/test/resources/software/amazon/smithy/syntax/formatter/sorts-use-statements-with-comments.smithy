// Comments that appear above USE_STATEMENTs are found in the WS tree before the statement. This gets tricky with
// the first use statement and the last use statement. The first use statement actually gets comments from the
// preceding NAMESPACE_STATEMENT. The last use statement actually provides comments for any subsequent shapes.
$version: "2.0"

namespace smithy.example

// This set of comments is actually in the BR->WS of the above NAMESPACE_STATEMENT.
// When moved, it needs to be removed from NAMESPACE_STATEMENT, and added to the USE_STATEMENT prior to where
// smithy.api#String is reorderd.
use smithy.api#String // Trailing smithy.api#String

// Comes before Integer 1
// Comes before Integer 2
use smithy.api#Integer // Trailing smithy.api#Integer

// Comes before required 1
// Comes before required 2
use smithy.api#required // Trailing smithy.api#required

// Comes before Long 1
// Comes before Long 2
use smithy.api#Long // Trailing smithy.api#Long

// These comments are actually part of the last USE_STATEMENT. When the last statement is moved, this comment must
// be removed from that statement and added to the updated last statement.
structure UseThem {
    @required
    string: String

    integer: Integer

    long: Long
}
