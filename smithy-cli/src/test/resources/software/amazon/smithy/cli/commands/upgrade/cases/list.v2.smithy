$version: "2.0"

namespace com.example

@default(0)
integer NonBoxedInteger

list SparseList {
    member: NonBoxedInteger,
}

@uniqueItems
list SparseSet {
    member: NonBoxedInteger,
}

list SingleLineList { member: NonBoxedInteger, }

@uniqueItems
list SingleLineSet { member: NonBoxedInteger }

  @uniqueItems
list SetWithIndentation { member: NonBoxedInteger }

@uniqueItems
list SetWithComment { // This comment is here
    member: NonBoxedInteger,
}

@tags(["set"]) @uniqueItems
list MultipleSetCharacters { member: NonBoxedInteger, }
