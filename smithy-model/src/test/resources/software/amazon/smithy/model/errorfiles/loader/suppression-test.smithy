$version: "2.0"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "NoListsPlease",
        message: "This list must not exist!",
        configuration: {
            selector: "list"
        }
    },
    {
        name: "EmitEachSelector",
        id: "ListMembersAreBadOkay",
        message: "Members? Yuck!",
        severity: "WARNING",
        configuration: {
            selector: "list > member"
        }
    },
    {
        // This one is suppressed.
        name: "EmitEachSelector",
        id: "SuppressedValidator.Foo",
        message: "This is suppressed",
        severity: "WARNING",
        configuration: {
            selector: "service"
        }
    },
    {
        name: "UnknownValidator1"
    },
    {
        name: "UnknownValidator2"
    }
]

metadata suppressions = [
    {
        id: "UnknownValidator_UnknownValidator2", // Matches any event not bound to a shape.
        namespace: "*",
        reason: "Please ignore this",
    },
    {
        id: "SuppressedValidator.Foo",
        namespace: "smithy.example.ignore.this.one", // matches nothing
    },
    {
        id: "SuppressedValidator.Foo",
        namespace: "smithy.example"
    },
]

namespace smithy.example

@suppress(["IgnoreMe"])
service MyService {
    version: "XYZ",
    operations: [GetFoo],
}

@suppress(["IgnoreMe.Too"])
operation GetFoo {
    input: GetFooInput,
    output: GetFooOutput
}

@input
structure GetFooInput {
    list1: List1,
    list2: List2,
}

@output
structure GetFooOutput {}

list List1 {
    member: String,
}

@suppress(["NoListsPlease"])
list List2 {
    @suppress(["ListMembersAreBadOkay"])
    member: String,
}
