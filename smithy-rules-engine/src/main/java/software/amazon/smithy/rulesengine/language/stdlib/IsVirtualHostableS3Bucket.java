package software.amazon.smithy.rulesengine.language.stdlib;

import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.utils.SmithyUnstableApi;

import java.util.Arrays;
import java.util.List;

@SmithyUnstableApi
public class IsVirtualHostableS3Bucket extends FunctionDefinition {
    public static final String ID = "aws.isVirtualHostableS3Bucket";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Type> arguments() {
        return Arrays.asList(Type.str(), Type.bool());
    }

    @Override
    public Type returnType() {
        return Type.bool();
    }

    @Override
    public Value eval(List<Value> arguments) {
        return Value.bool(true);
//        String hostLabel = arguments.get(0).expectString();
//        boolean allowDots = arguments.get(1).expectBool();
//        if (allowDots) {
//            return Value.bool(hostLabel.matches("[a-z\\d][a-z\\d\\-.]{1,61}[a-z\\d]"));
//        } else {
//            return Value.bool(hostLabel.matches("[a-z\\d][a-z\\d\\-]{1,61}[a-z\\d]"));
//        }
    }
}
