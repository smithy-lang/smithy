from docutils import nodes
from docutils.parsers.rst import Directive

from sphinx.locale import _

SMITHY_TRAITS_ATTR = "smithy_traits"


# Placeholder node that will be replaced with the trait index.
class SmithyTraitIndexNode(nodes.General, nodes.Element):
    pass


# Directive that places a SmithyTraitIndexNode in the document.
# Usage: .. smithy-trait-index::
class SmithyTraitIndexDirective(Directive):
    def run(self):
        return [SmithyTraitIndexNode("")]


# Directive that marks the location of a smithy trait in the docs.
# Usage: .. smithy-trait: ns.example#shapeId
class SmithyTraitDirective(Directive):

    # Makes the shape id argument required
    required_arguments = 1

    def run(self):
        env = self.state.document.settings.env
        shape_id = self.arguments[0]

        # Make the shape id url-friendly
        stripped_shape_id = shape_id.strip().replace("#", "-").replace(".", "-").lower()

        # Create a new target node that can be referenced
        target_id = stripped_shape_id + "-trait"
        target_node = nodes.target("", "", ids=[target_id])

        # Add a list of the smithy trait targets to the envirnonment so that
        # they can be referenced later when building the trait index.
        if not hasattr(env, SMITHY_TRAITS_ATTR):
            env.smithy_traits = []

        env.smithy_traits.append(
            {
                "docname": env.docname,
                "target": target_node,
                "shape_id": shape_id,
            }
        )

        return [target_node]


# If the source file changes, remove any cached smithy traits for it. If there
# are any remaining in the file itself, they'll be re-added in the normal way.
def purge_smithy_traits(app, env, docname):
    if not hasattr(env, SMITHY_TRAITS_ATTR):
        return
    env.smithy_traits = [t for t in env.smithy_traits if t["docname"] != docname]


# In the case of a parallel build, there will be multiple envs. This handles
# merging all the smithy traits into a single list at the end.
def merge_smithy_traits(app, env, docnames, other):
    if not hasattr(env, SMITHY_TRAITS_ATTR):
        env.smithy_traits = []
    if hasattr(other, SMITHY_TRAITS_ATTR):
        env.smithy_traits.extend(other.smithy_traits)


# Generates a list of links to known smithy traits, replacing any
# SmithyTraitIndexNodes
def process_smithy_nodes(app, doctree, fromdocname):
    env = app.builder.env

    if not hasattr(env, SMITHY_TRAITS_ATTR):
        env.smithy_traits = []

    for node in doctree.traverse(SmithyTraitIndexNode):
        new_index_node = nodes.bullet_list()

        smithy_traits = sorted(env.smithy_traits, key=lambda x: x["shape_id"])

        for smithy_trait_info in smithy_traits:
            shape_id = smithy_trait_info["shape_id"]

            # Create a reference node, which becomes a link with the text being
            # the shape id.
            ref_node = nodes.reference("", "")
            inner_node = nodes.emphasis(_(shape_id), _(shape_id))
            ref_node["refdocname"] = smithy_trait_info["docname"]
            ref_node["refuri"] = app.builder.get_relative_uri(
                fromdocname, smithy_trait_info["docname"]
            )
            ref_node["refuri"] += "#" + smithy_trait_info["target"]["refid"]
            ref_node.append(inner_node)

            # Wrap the reference in a paragraph and construct a list item from it.
            para = nodes.paragraph()
            para += ref_node
            list_item = nodes.list_item()
            list_item += para

            new_index_node.append(list_item)

        node.replace_self(new_index_node)


def setup_smithy_trait_index(app):
    app.add_node(SmithyTraitIndexNode)
    app.add_directive("smithy-trait", SmithyTraitDirective)
    app.add_directive("smithy-trait-index", SmithyTraitIndexDirective)
    app.connect("doctree-resolved", process_smithy_nodes)
    app.connect("env-purge-doc", purge_smithy_traits)
    app.connect("env-merge-info", merge_smithy_traits)
