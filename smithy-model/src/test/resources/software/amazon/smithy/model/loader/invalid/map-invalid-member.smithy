// Parse error at line 7, column 3 near `fuzz`: Expected: '}', but found 'f' | Model
namespace com.foo

map MyMap {
  key: smithy.api#String,
  value: smithy.api#String,
  fuzz: smithy.api#String,
}
