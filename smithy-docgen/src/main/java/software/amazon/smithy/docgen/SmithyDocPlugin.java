/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.docgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.codegen.core.directed.CodegenDirector;
import software.amazon.smithy.docgen.validation.DocValidationEventDecorator;
import software.amazon.smithy.docgen.writers.DocWriter;
import software.amazon.smithy.linters.InputOutputStructureReuseValidator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationEventDecorator;
import software.amazon.smithy.model.validation.suppressions.ModelBasedEventDecorator;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Generates API documentation from a Smithy model.
 */
@SmithyInternalApi
public final class SmithyDocPlugin implements SmithyBuildPlugin {

    private static final Logger LOGGER = Logger.getLogger(SmithyDocPlugin.class.getName());

    @Override
    public String getName() {
        return "docgen";
    }

    @Override
    public void execute(PluginContext pluginContext) {
        LOGGER.fine("Beginning documentation generation.");
        CodegenDirector<DocWriter, DocIntegration, DocGenerationContext, DocSettings> runner = new CodegenDirector<>();

        runner.directedCodegen(new DirectedDocGen());
        runner.integrationClass(DocIntegration.class);
        runner.fileManifest(pluginContext.getFileManifest());
        runner.model(getValidatedModel(pluginContext.getModel()).unwrap());
        DocSettings settings = runner.settings(DocSettings.class, pluginContext.getSettings());
        runner.service(settings.service());
        runner.performDefaultCodegenTransforms();
        runner.run();
        LOGGER.fine("Finished documentation generation.");
    }

    private ValidatedResult<Model> getValidatedModel(Model model) {
        // This decorator will add context for why these are particularly important for docs.
        ValidationEventDecorator eventDecorator = new DocValidationEventDecorator();

        // This will discover and apply suppressions from the model.
        Optional<ValidationEventDecorator> modelDecorator = new ModelBasedEventDecorator()
                .createDecorator(model)
                .getResult();
        if (modelDecorator.isPresent()) {
            eventDecorator = ValidationEventDecorator.compose(List.of(modelDecorator.get(), eventDecorator));
        }

        var events = new ArrayList<ValidationEvent>();
        for (var event : validate(model)) {
            if (eventDecorator.canDecorate(event)) {
                event = eventDecorator.decorate(event);
            }
            events.add(event);
        }
        return new ValidatedResult<>(model, events);
    }

    private List<ValidationEvent> validate(Model model) {
        return new InputOutputStructureReuseValidator().validate(model);
    }
}
