$version: "1.0"

namespace com.example

@enum([
    {
        name: "FOO",
        value: "foo",
    },
    {
        name: "BAR",
        value: "bar",
    }
])
@internal
string TraitAfterEnum

@internal
@enum([
    {
        name: "FOO",
        value: "foo",
    },
    {
        name: "BAR",
        value: "bar",
    }
])
string TraitBeforeEnum

@enum([
    {
        name: "FOO",
        value: "foo",
    },
    {
        name: "BAR",
        value: "bar",
    }
])
@internal()
string AnnotationTraitWithParens
