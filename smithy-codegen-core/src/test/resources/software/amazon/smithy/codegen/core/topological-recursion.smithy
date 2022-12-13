$version: "2.0"

namespace smithy.example

service Example {
    version: "1.0.0",
    operations: [GetFoo]
}

operation GetFoo {
    input: GetFooInput
}

structure GetFooInput {
    foo: User
}

structure User {
    recursiveUser: User,
    recursiveList: UsersList,
    recursiveMap: UsersMap,
    notRecursive: NonRecursiveList,
}

list UsersList {
    member: User
}

map UsersMap {
    key: MyString,
    value: User
}

list NonRecursiveList {
    member: NonRecursive,
}

structure NonRecursive {
    foo: MyString
}

string MyString
