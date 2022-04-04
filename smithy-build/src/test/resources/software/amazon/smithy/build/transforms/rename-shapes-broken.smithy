namespace ns.foo

string MyString

@protoReservedFields([1])
structure MyStruct {
    value: MyString
}

@trait(selector: "structure")
list protoReservedFields {
  member: Integer
}