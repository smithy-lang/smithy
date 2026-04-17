$version: "2.1"

namespace ns.foo

@trait
list ListTrait {
    member: String
}

@trait
@uniqueItems
list UniqueItemsListTrait {
    member: String
}

@ListTrait([])
@UniqueItemsListTrait([])
string Bar

@ListTrait([
    "first"
    "second"
])
@UniqueItemsListTrait([
    "first"
    "second"
])
string Foo
