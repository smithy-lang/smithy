package software.amazon.smithy.jmespath.type;

public class ArrayType implements Type {

    private final Type member;

    public ArrayType(Type member) {
        this.member = member;
    }
}
