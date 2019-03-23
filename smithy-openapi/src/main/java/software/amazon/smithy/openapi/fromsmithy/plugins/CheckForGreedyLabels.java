package software.amazon.smithy.openapi.fromsmithy.plugins;

import java.util.logging.Logger;
import software.amazon.smithy.openapi.OpenApiConstants;
import software.amazon.smithy.openapi.OpenApiException;
import software.amazon.smithy.openapi.fromsmithy.Context;
import software.amazon.smithy.openapi.fromsmithy.SmithyOpenApiPlugin;
import software.amazon.smithy.openapi.model.OpenApi;

/**
 * Checks for greedy labels and fails/warns depending on configuration.
 *
 * <p>Some vendors like API Gateway support greedy labels in the form of
 * "{foo+}", while others do not.
 */
public class CheckForGreedyLabels implements SmithyOpenApiPlugin {
    private static final Logger LOGGER = Logger.getLogger(CheckForGreedyLabels.class.getName());

    @Override
    public OpenApi after(Context context, OpenApi openApi) {
        var forbid = context.getConfig().getBooleanMemberOrDefault(OpenApiConstants.FORBID_GREEDY_LABELS);

        for (var path : openApi.getPaths().keySet()) {
            // Throw an exception or warning when greedy URI labels are found in the path.
            if (path.contains("+}")) {
                String message = "Greedy URI path label found in path `" + path + "`. Not all OpenAPI "
                                 + "tools support this style of URI labels. Greedy URI labels are expected "
                                 + "to capture all remaining components of a URI, so if a tool does not "
                                 + "support them, the API will not function properly.";
                if (forbid) {
                    throw new OpenApiException(message);
                } else {
                    LOGGER.warning(message);
                }
            }
        }

        return openApi;
    }
}
