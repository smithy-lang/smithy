$version: "1.0"

namespace com.example

integer NonBoxedInteger

list SparseList {
    @box
    member: NonBoxedInteger,
}

set SparseSet {
    @box
    member: NonBoxedInteger,
}

list SingleLineList { member: NonBoxedInteger, }

set SingleLineSet { member: NonBoxedInteger }

  set SetWithIndentation { member: NonBoxedInteger }

set SetWithComment { // This comment is here
    member: NonBoxedInteger,
}

@tags(["set"]) set MultipleSetCharacters { member: NonBoxedInteger, }
