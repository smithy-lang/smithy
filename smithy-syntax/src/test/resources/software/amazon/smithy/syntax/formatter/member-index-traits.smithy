$version:"2.1"
namespace smithy.example
use smithy.protocols#idx
structure Indexed{
// keep this comment
@required
@idx(10)
first:String
@idx(1)
second:String
}
