// Parse error at line 7, column 7 near `: `: Unexpected member of com.foo#MyMap: 'fuzz'
namespace com.foo

map MyMap {
  key: smithy.api#String,
  value: smithy.api#String,
  fuzz: smithy.api#String,
}
