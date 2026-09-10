/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * End-to-end verification that endpoint rule-set functions contributed by an
 * {@link software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension} in
 * {@code smithy-aws-endpoints} (here, {@code aws.partition}) are resolved based on the classloader
 * supplied to {@link Model#assembler(ClassLoader)}, not the classloader that loaded the rules
 * engine.
 *
 * <p>This reproduces the failure seen when a model is assembled against a dependency-closure
 * classloader: the AWS endpoints extension lives only in that closure, and the rules engine used to
 * discover functions from its own classloader, so {@code aws.partition} was reported as an invalid
 * function. The test drives both directions:
 *
 * <ul>
 *   <li>Assembling with a classloader that can see the extension resolves the rule-set with no
 *       errors.</li>
 *   <li>Assembling with a classloader that hides the extension reproduces the original failure,
 *       proving the resolution really is driven by the supplied classloader.</li>
 * </ul>
 */
class EndpointRuleSetClassLoaderIntegrationTest {

    private static final String PARTITION_MODEL =
            "software/amazon/smithy/rulesengine/aws/language/functions/errorfiles/valid/partition-fn.smithy";

    private static final String EXTENSION_SERVICE =
            "META-INF/services/software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension";

    private static final String NESTED_FN_MODEL =
            "software/amazon/smithy/rulesengine/aws/language/functions/nested-parse-arn.smithy";

    // The rules-engine trait definitions (smithy.rules#endpointRuleSet, #endpointBdd,
    // #clientContextParams, ...). Added explicitly instead of discoverModels() so the S3 model on
    // the integration-test classpath is not pulled in, which would add unrelated aws.partition
    // errors and mask the specific failures under assertion.
    private static final String RULES_TRAITS_MODEL = "META-INF/smithy/smithy.rules.smithy";

    private static final String AWS_EXTENSION_PACKAGE = "software.amazon.smithy.rulesengine.aws.";

    @Test
    void resolvesAwsPartitionWhenExtensionVisibleToSuppliedClassLoader() {
        ClassLoader loader = EndpointRuleSetClassLoaderIntegrationTest.class.getClassLoader();

        ValidatedResult<Model> result = Model.assembler(loader)
                .addImport(loader.getResource(RULES_TRAITS_MODEL))
                .addImport(loader.getResource(PARTITION_MODEL))
                .assemble();

        List<ValidationEvent> errors = result.getValidationEvents(Severity.ERROR);
        assertThat(errorMessages(errors), is(empty()));
    }

    @Test
    void reportsAwsPartitionInvalidWhenExtensionHiddenFromSuppliedClassLoader() {
        ClassLoader appLoader = EndpointRuleSetClassLoaderIntegrationTest.class.getClassLoader();
        // A classloader that can load the rules engine and model classes but hides the AWS
        // endpoints extension's service registration, mimicking a closure that does not include
        // the smithy-aws-endpoints endpoint functions.
        ClassLoader hidingLoader = new AwsEndpointsHidingClassLoader(appLoader);

        // With the extension hidden from the supplied classloader, aws.partition cannot be
        // resolved. The failure may surface as an ERROR event or a thrown exception depending on
        // validator scheduling; assembleFailureText captures both.
        String failure = assembleFailureText(hidingLoader, PARTITION_MODEL, null);

        assertThat(failure, containsString("`aws.partition` is not a valid function"));
    }

    @Test
    void resolvesNestedFunctionArgumentFromSuppliedClassLoader() {
        // aws.parseArn is used nested as an argument to getAttr. This guards that nested function
        // arguments are resolved against the supplied classloader, not only the top-level fn.
        ClassLoader loader = EndpointRuleSetClassLoaderIntegrationTest.class.getClassLoader();

        ValidatedResult<Model> result = Model.assembler(loader)
                .addImport(loader.getResource(RULES_TRAITS_MODEL))
                .addImport(loader.getResource(NESTED_FN_MODEL))
                .assemble();

        assertThat(errorMessages(result.getValidationEvents(Severity.ERROR)), is(empty()));
    }

    @Test
    void reportsNestedFunctionInvalidWhenExtensionHiddenFromSuppliedClassLoader() {
        ClassLoader appLoader = EndpointRuleSetClassLoaderIntegrationTest.class.getClassLoader();
        ClassLoader hidingLoader = new AwsEndpointsHidingClassLoader(appLoader);

        // The nested aws.parseArn must fail to resolve when the extension is hidden, proving nested
        // arguments follow the supplied classloader too.
        String failure = assembleFailureText(hidingLoader, NESTED_FN_MODEL, null);

        assertThat(failure, containsString("`aws.parseArn` is not a valid function"));
    }

    @Test
    void resolvesAwsPartitionInBddTraitFromSuppliedClassLoader() {
        // The @endpointBdd trait path must also resolve functions against the supplied classloader.
        ClassLoader loader = EndpointRuleSetClassLoaderIntegrationTest.class.getClassLoader();

        ValidatedResult<Model> result = Model.assembler(loader)
                .addImport(loader.getResource(RULES_TRAITS_MODEL))
                .addUnparsedModel("bdd.smithy", bddModel(loader))
                .assemble();

        assertThat(errorMessages(result.getValidationEvents(Severity.ERROR)), is(empty()));
    }

    @Test
    void reportsAwsPartitionInvalidInBddTraitWhenExtensionHidden() {
        ClassLoader appLoader = EndpointRuleSetClassLoaderIntegrationTest.class.getClassLoader();
        ClassLoader hidingLoader = new AwsEndpointsHidingClassLoader(appLoader);
        String model = bddModel(appLoader);

        String failure = assembleFailureText(hidingLoader, null, model);

        assertThat(failure, containsString("`aws.partition` is not a valid function"));
    }

    // Builds a model whose service carries an @endpointBdd trait compiled from the aws.partition
    // rule-set. The BDD is generated (and serialized) with the given classloader so the extension
    // is available while producing the fixture; the assertions above then control which classloader
    // is used to re-parse it.
    private static String bddModel(ClassLoader loader) {
        Model ruleSetModel = Model.assembler(loader)
                .addImport(loader.getResource(RULES_TRAITS_MODEL))
                .addImport(loader.getResource(PARTITION_MODEL))
                .assemble()
                .unwrap();
        EndpointRuleSetTrait ruleSetTrait = ruleSetModel
                .getServiceShapesWithTrait(EndpointRuleSetTrait.class)
                .iterator()
                .next()
                .expectTrait(EndpointRuleSetTrait.class);
        EndpointBddTrait bddTrait = EndpointBddTrait.from(Cfg.from(ruleSetTrait.getEndpointRuleSet()));
        String bddJson = Node.printJson(bddTrait.toNode());
        return "$version: \"2.0\"\n"
                + "namespace example\n"
                + "use smithy.rules#endpointBdd\n"
                + "@endpointBdd(" + bddJson + ")\n"
                + "service BddService {}\n";
    }

    private static List<String> errorMessages(List<ValidationEvent> events) {
        return events.stream().map(ValidationEvent::getMessage).collect(Collectors.toList());
    }

    // Assembles and returns all failure text, whether the failure surfaces as ERROR validation
    // events or as an exception thrown from assemble(). Endpoint function-resolution failures can
    // surface either way depending on which validator materializes the trait first (validators run
    // on a parallel stream), so a robust assertion must consider both.
    private static String assembleFailureText(ClassLoader loader, String modelResource, String unparsedModel) {
        StringBuilder sb = new StringBuilder();
        try {
            ModelAssembler assembler = Model.assembler(loader)
                    .addImport(loader.getResource(RULES_TRAITS_MODEL));
            if (modelResource != null) {
                assembler.addImport(loader.getResource(modelResource));
            }
            if (unparsedModel != null) {
                assembler.addUnparsedModel("test.smithy", unparsedModel);
            }
            ValidatedResult<Model> result = assembler.assemble();
            for (ValidationEvent event : result.getValidationEvents(Severity.ERROR)) {
                sb.append(event.getMessage()).append('\n');
            }
        } catch (RuntimeException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                sb.append(t.getMessage()).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * A classloader that behaves like its parent, except it hides the {@code smithy-aws-endpoints}
     * {@link software.amazon.smithy.rulesengine.language.EndpointRuleSetExtension} service
     * registration. This makes {@code aws.partition} undiscoverable through it, reproducing a
     * dependency closure that does not contribute the AWS endpoint functions, while leaving every
     * other service (traits, validators) and class loadable so the model still assembles far enough
     * to reach function resolution.
     *
     * <p>Only the AWS extension's service manifest (the one that lists a class in
     * {@code software.amazon.smithy.rulesengine.aws.*}) is filtered out; the rules engine's own
     * {@code CoreExtension} manifest, which lives in a separate jar under the same resource path, is
     * left intact so core functions such as {@code getAttr} still resolve.
     */
    private static final class AwsEndpointsHidingClassLoader extends ClassLoader {
        AwsEndpointsHidingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            Enumeration<URL> all = super.getResources(name);
            if (!name.equals(EXTENSION_SERVICE)) {
                return all;
            }
            // Keep only manifests that do not register an AWS endpoints extension.
            List<URL> kept = new ArrayList<>();
            while (all.hasMoreElements()) {
                URL url = all.nextElement();
                if (!registersAwsExtension(url)) {
                    kept.add(url);
                }
            }
            return Collections.enumeration(kept);
        }

        private static boolean registersAwsExtension(URL url) {
            try (InputStream in = url.openStream()) {
                String contents = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return contents.contains(AWS_EXTENSION_PACKAGE);
            } catch (IOException e) {
                return false;
            }
        }
    }
}
