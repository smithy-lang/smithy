package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.RecordValue;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;

public class AwsPartitionFunctionTest {
    @Test
    public void eval() {
        RecordValue result = evalWithRegion("us-west-2");

        assertThat(result.get(AwsPartition.DNS_SUFFIX).expectStringValue().getValue(), not(equalTo("")));
        assertThat(result.get(AwsPartition.DUAL_STACK_DNS_SUFFIX).expectStringValue().getValue(), not(equalTo("")));
        assertThat(result.get(AwsPartition.SUPPORTS_FIPS).expectBooleanValue().getValue(), equalTo(true));
        assertThat(result.get(AwsPartition.SUPPORTS_DUAL_STACK).expectBooleanValue().getValue(), equalTo(true));
        assertThat(result.get(AwsPartition.IMPLICIT_GLOBAL_REGION).expectStringValue().getValue(), not(equalTo("")));
    }

    @Test
    public void eval_enumeratedRegion_inferredIsFalse() {
        RecordValue result = evalWithRegion("us-west-1");

        assertThat(result.get(AwsPartition.INFERRED).expectBooleanValue().getValue(), equalTo(false));
    }

    @Test
    public void eval_regionNotEnumerated_inferredIsTrue() {
        RecordValue result = evalWithRegion("us-west-3");

        assertThat(result.get(AwsPartition.INFERRED).expectBooleanValue().getValue(), equalTo(true));
    }


    private RecordValue evalWithRegion(String region) {
        AwsPartition fn = AwsPartition.ofExpressions(Expression.of(region));
        return fn.accept(new RuleEvaluator()).expectRecordValue();
    }
}
