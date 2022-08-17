$version: "2"

metadata validators = [
    {
        name: "EmitEachSelector",
        id: "_no.LiSt_s.__ple.ase__",
        message: "This list must not exist!",
        configuration: {
            selector: "list"
        }
    },
    {
        name: "EmitEachSelector",
        id: "_list_members_.are_bad_okay",
        message: "Members? Yuck!",
        severity: "WARNING",
        configuration: {
            selector: "list > member"
        }
    },
    {
        // This one is suppressed.
        name: "EmitEachSelector",
        id: "Suppressed_Validator._test",
        message: "This is suppressed",
        severity: "WARNING",
        configuration: {
            selector: "service"
        }
    },
    {
        name: "_Unknown.Validato._r1._a"
    },
    {
        name: "_Unknown.Validato._r2._b"
    }
]

metadata suppressions = [
    {
        id: "UnknownValidator__Unknown.Validato._r2._b", // Matches any event not bound to a shape.
        namespace: "*",
        reason: "Please ignore this",
    },
    {
        id: "Suppressed_Validator._test",
        namespace: "smithy.example.ignore.this.one", // matches nothing
    },
    {
        id: "Suppressed_Validator._test",
        namespace: "smithy.example"
    },
]

namespace smithy.example

@suppress(["_Ingore_Me.today"])
service MyService {
    version: "XYZ",
    operations: [GetFoo],
}

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

@suppress(["_no.LiSt_s.__ple.ase__"])
list List2 {
    @suppress(["_list_members_.are_bad_okay"])
    member: String,
}
