$version: "2.0"

namespace test.smithy.traitcodegen.uniqueitems

@trait
@uniqueItems
list StringSetTrait {
    member: String
}
