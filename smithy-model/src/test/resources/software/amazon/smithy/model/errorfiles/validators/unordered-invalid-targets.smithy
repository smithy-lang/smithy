$version: "2.0"

namespace smithy.example

// Valid: the trait applies to a list shape.
@unordered
list Audiences {
    member: String
}

// Valid: the trait applies to a member that targets a list shape, which is how
// two uses of a shared list can disagree about whether its order matters.
structure HookConfiguration {
    orderedHooks: Hooks

    @unordered
    eligibleHooks: Hooks
}

list Hooks {
    member: String
}

// Invalid: not a list.
@unordered
map Lookup {
    key: String
    value: String
}

// Invalid: not a list.
@unordered
string Name

// Invalid: a member, but one that targets a string rather than a list.
structure Invalid {
    @unordered
    name: String
}
