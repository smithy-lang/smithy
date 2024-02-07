package software.amazon.smithy.aws.smoketests.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.smoketests.traits.Expectation;
import software.amazon.smithy.smoketests.traits.SmokeTestCase;
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait;

public class AwsSmokeTestModelTest {
    @Test
    public void determinesIfTestCaseHasAwsVendorParams() {
        SmokeTestCase withAwsVendorParamsShape = SmokeTestCase.builder()
                .id("foo")
                .expectation(Expectation.success())
                .vendorParamsShape(AwsVendorParams.ID)
                .build();
        SmokeTestCase withNoVendorParamsShape = withAwsVendorParamsShape.toBuilder()
                .vendorParamsShape(null)
                .build();
        SmokeTestCase withOtherVendorParamsShape = withNoVendorParamsShape.toBuilder()
                .vendorParamsShape(S3VendorParams.ID)
                .build();

        assertThat(AwsSmokeTestModel.hasAwsVendorParams(withAwsVendorParamsShape), is(true));
        assertThat(AwsSmokeTestModel.hasAwsVendorParams(withNoVendorParamsShape), is(false));
        assertThat(AwsSmokeTestModel.hasAwsVendorParams(withOtherVendorParamsShape), is(false));
    }

    @Test
    public void determinesIfTestCaseHasS3VendorParams() {
        SmokeTestCase withS3VendorParamsShape = SmokeTestCase.builder()
                .id("foo")
                .expectation(Expectation.success())
                .vendorParamsShape(S3VendorParams.ID)
                .build();
        SmokeTestCase withNoVendorParamsShape = withS3VendorParamsShape.toBuilder()
                .vendorParamsShape(null)
                .build();
        SmokeTestCase withOtherVendorParamsShape = withNoVendorParamsShape.toBuilder()
                .vendorParamsShape(AwsVendorParams.ID)
                .build();

        assertThat(AwsSmokeTestModel.hasS3VendorParams(withS3VendorParamsShape), is(true));
        assertThat(AwsSmokeTestModel.hasS3VendorParams(withNoVendorParamsShape), is(false));
        assertThat(AwsSmokeTestModel.hasS3VendorParams(withOtherVendorParamsShape), is(false));
    }

    @Test
    public void canGetAwsVendorParamsFromTestCase() {
        SmokeTestCase withoutVendorParams = SmokeTestCase.builder()
                .id("foo")
                .expectation(Expectation.success())
                .vendorParamsShape(AwsVendorParams.ID)
                .build();
        SmokeTestCase withNoVendorParamsShape = withoutVendorParams.toBuilder()
                .vendorParamsShape(null)
                .build();
        SmokeTestCase withOtherVendorParamsShape = withNoVendorParamsShape.toBuilder()
                .vendorParamsShape(S3VendorParams.ID)
                .build();

        assertThat(AwsSmokeTestModel.getAwsVendorParams(withoutVendorParams), not(equalTo(Optional.empty())));
        assertThat(AwsSmokeTestModel.getAwsVendorParams(withNoVendorParamsShape), equalTo(Optional.empty()));
        assertThat(AwsSmokeTestModel.getAwsVendorParams(withOtherVendorParamsShape), equalTo(Optional.empty()));
    }

    @Test
    public void fillsInDefaultsForAwsVendorParams() {
        AwsVendorParams vendorParams = AwsSmokeTestModel.getAwsVendorParams(SmokeTestCase.builder()
                .id("foo")
                .expectation(Expectation.success())
                .vendorParamsShape(AwsVendorParams.ID)
                .build()).get();

        assertThat(vendorParams.getRegion(), equalTo("us-west-2"));
        assertThat(vendorParams.getSigv4aRegionSet(), equalTo(Optional.empty()));
        assertThat(vendorParams.getUri(), equalTo(Optional.empty()));
        assertThat(vendorParams.useFips(), is(false));
        assertThat(vendorParams.useDualstack(), is(false));
        assertThat(vendorParams.useAccountIdRouting(), is(true));
    }

    @Test
    public void fillsInDefaultsForS3VendorParams() {
        S3VendorParams vendorParams = AwsSmokeTestModel.getS3VendorParams(SmokeTestCase.builder()
                .id("foo")
                .expectation(Expectation.success())
                .vendorParamsShape(S3VendorParams.ID)
                .build()).get();

        assertThat(vendorParams.getRegion(), equalTo("us-west-2"));
        assertThat(vendorParams.getSigv4aRegionSet(), equalTo(Optional.empty()));
        assertThat(vendorParams.getUri(), equalTo(Optional.empty()));
        assertThat(vendorParams.useFips(), is(false));
        assertThat(vendorParams.useDualstack(), is(false));
        assertThat(vendorParams.useAccountIdRouting(), is(true));

        assertThat(vendorParams.useAccelerate(), is(false));
        assertThat(vendorParams.useGlobalEndpoint(), is(false));
        assertThat(vendorParams.forcePathStyle(), is(false));
        assertThat(vendorParams.useArnRegion(), is(true));
        assertThat(vendorParams.useMultiRegionAccessPoints(), is(true));
    }

    @Test
    public void loadsAwsVendorParamsFromModel() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params.smithy"))
                .assemble()
                .unwrap();
        SmokeTestsTrait trait = model.expectShape(ShapeId.from("com.foo#GetFoo")).expectTrait(SmokeTestsTrait.class);
        SmokeTestCase awsCase = trait.getTestCases().stream()
                .filter(testCase -> testCase.getId().equals("AwsVendorParamsCase"))
                .findAny()
                .get();

        assertThat(AwsSmokeTestModel.hasAwsVendorParams(awsCase), is(true));
        assertThat(AwsSmokeTestModel.getAwsVendorParams(awsCase), not(equalTo(Optional.empty())));
        AwsVendorParams vendorParams = AwsSmokeTestModel.getAwsVendorParams(awsCase).get();
        assertThat(vendorParams.getRegion(), equalTo("us-east-1"));
        assertThat(vendorParams.getSigv4aRegionSet(), not(equalTo(Optional.empty())));
        assertThat(vendorParams.getSigv4aRegionSet().get(), containsInAnyOrder("us-east-1"));
        assertThat(vendorParams.getUri(), equalTo(Optional.of("foo")));
        assertThat(vendorParams.useFips(), is(true));
        assertThat(vendorParams.useDualstack(), is(true));
        assertThat(vendorParams.useAccountIdRouting(), is(false));
    }

    @Test
    public void loadsS3VendorParamsFromModel() {
        Model model = Model.assembler()
                .discoverModels()
                .addImport(getClass().getResource("vendor-params.smithy"))
                .assemble()
                .unwrap();
        SmokeTestCase s3Case = model.expectShape(ShapeId.from("com.foo#GetFoo"))
                .expectTrait(SmokeTestsTrait.class)
                .getTestCases()
                .stream()
                .filter(testCase -> testCase.getId().equals("S3VendorParamsCase"))
                .findAny()
                .get();

        assertThat(AwsSmokeTestModel.hasS3VendorParams(s3Case), is(true));
        assertThat(AwsSmokeTestModel.getS3VendorParams(s3Case), not(equalTo(Optional.empty())));
        S3VendorParams vendorParams = AwsSmokeTestModel.getS3VendorParams(s3Case).get();
        assertThat(vendorParams.getRegion(), equalTo("us-east-2"));
        assertThat(vendorParams.getSigv4aRegionSet(), not(equalTo(Optional.empty())));
        assertThat(vendorParams.getSigv4aRegionSet().get(), containsInAnyOrder("us-east-2"));
        assertThat(vendorParams.getUri(), equalTo(Optional.of("bar")));
        assertThat(vendorParams.useFips(), is(true));
        assertThat(vendorParams.useDualstack(), is(true));
        assertThat(vendorParams.useAccountIdRouting(), is(false));
        assertThat(vendorParams.useAccelerate(), is(true));
        assertThat(vendorParams.useGlobalEndpoint(), is(true));
        assertThat(vendorParams.forcePathStyle(), is(true));
        assertThat(vendorParams.useArnRegion(), is(false));
        assertThat(vendorParams.useMultiRegionAccessPoints(), is(false));
    }
}
