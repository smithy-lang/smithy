$version: "2.0"

namespace ns.foo

@output
structure HttpOperationOutput {

    @required
    @httpResponseCode
    @httpLabel
    @httpQuery("foo")
    @httpHeader("x-amz-foo")
    Int: Integer

}
