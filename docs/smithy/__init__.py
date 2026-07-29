from docutils import nodes
from sphinx import addnodes

from .traitindex import setup_smithy_trait_index
from .redirects import setup as setup_redirects


def setup(app):
    app.add_node(
        addnodes.productionlist,
        override=True,
        html=(visit_productionlist, None),
    )
    setup_smithy_trait_index(app)
    setup_redirects(app)


def visit_productionlist(translator, node):
    """Render production lists using ABNF rather than BNF notation."""
    node.get('classes', []).extend(("productionlist", "highlight"))
    translator.body.append(translator.starttag(node, 'pre'))
    has_token = False

    for production in node:
        children = iter(production.children)
        first_child = next(children)

        if production['tokenname']:
            if has_token:
                translator.body.append('\n')
            has_token = True

            # Sphinx includes the token name and " ::= " separator as the
            # first two children. Keep the name so its cross-reference target
            # is preserved, but replace the BNF separator with ABNF.
            first_child.walkabout(translator)
            separator = next(children)
            translator.body.append(separator.astext().replace("::=", "="))
        else:
            # Sphinx aligns continuation lines with the BNF right-hand side.
            # Remove the two columns dropped when "::=" becomes "=".
            translator.body.append(first_child.astext()[:-2])

        for child in children:
            child.walkabout(translator)
        translator.body.append('\n')

    translator.body.append('</pre>\n')
    raise nodes.SkipNode
