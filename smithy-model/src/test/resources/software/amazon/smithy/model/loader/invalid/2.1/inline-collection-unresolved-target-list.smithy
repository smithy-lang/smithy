// member shape targets an unresolved shape `smithy.example#Missing`
$version: "2.1"

namespace smithy.example

structure S {
    inlineList: [Missing]
}
