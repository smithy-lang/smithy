$version: "2.0"

namespace test.smithy.traitcodegen.conflicts

use test.smithy.traitcodegen.conflicts#ListOfList
use test.smithy.traitcodegen.conflicts#SetOfSet

@trait
list ListWithNameConflict {
    member: ListOfList
}

@trait
@uniqueItems
list SetWithNameConflict {
    member: SetOfSet
}
