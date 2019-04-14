package software.amazon.smithy.openapi.fromsmithy.mappers;

import java.util.List;
import java.util.logging.Logger;
import software.amazon.smithy.model.knowledge.HttpBinding;
import software.amazon.smithy.model.knowledge.HttpBindingIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.OpenApiMapper;
import software.amazon.smithy.openapi.fromsmithy.OpenApiProtocol;
import software.amazon.smithy.openapi.model.OpenApi;

/**
 * Checks for prefix headers in the input or output of an operation,
 * and handles the settings for throwing an exception or warning if
 * they're found.
 *
 * <p>Custom values for this setting need to be handled in a
 * {@link OpenApiProtocol}.
 */
public class CheckForPrefixHeaders implements OpenApiMapper {
    private static final Logger LOGGER = Logger.getLogger(CheckForGreedyLabels.class.getName());

    @Override
    public byte getOrder() {
        return -128;
    }

    @Override
    public void before(Context context, OpenApi.Builder builder) {
        HttpBindingIndex httpBindings = context.getModel().getKnowledge(HttpBindingIndex.class);
        context.getModel().getShapeIndex().shapes(OperationShape.class).forEach(operation -> {
            check(context, httpBindings.getRequestBindings(operation, HttpBinding.Location.PREFIX_HEADERS));
            checkForResponseHeaders(context, httpBindings, operation);
            operation.getErrors().forEach(error -> checkForResponseHeaders(context, httpBindings, error));
        });
    }

    private void checkForResponseHeaders(Context context, HttpBindingIndex bindingIndex, ToShapeId shapeId) {
        check(context, bindingIndex.getResponseBindings(shapeId, HttpBinding.Location.PREFIX_HEADERS));
    }

    private void check(Context context, List<HttpBinding> bindings) {
        String setting = context.getConfig().getStringMemberOrDefault(
                OpenApiConstants.ON_HTTP_PREFIX_HEADERS, OpenApiConstants.ON_HTTP_PREFIX_HEADERS_FAIL);

        for (HttpBinding binding : bindings) {
            switch (setting) {
                case OpenApiConstants.ON_HTTP_PREFIX_HEADERS_WARN:
                    LOGGER.warning(createMessage(binding));
                    break;
                case OpenApiConstants.ON_HTTP_PREFIX_HEADERS_FAIL:
                    throw new OpenApiException(createMessage(binding));
                default:
                    break;
            }
        }
    }

    private static String createMessage(HttpBinding binding) {
        MemberShape member = binding.getMember();
        return String.format(
                "The `httpPrefixHeaders` trait is not supported by OpenAPI and was found on `%s`", member.getId());
    }
}
