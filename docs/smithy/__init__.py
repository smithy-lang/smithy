import re
from sphinx.writers.html import HTMLTranslator as SphinxHTMLTranslator
from docutils.writers.html4css1 import HTMLTranslator as BaseTranslator
from docutils import nodes

from sphinx.locale import _


def setup(app):
    app.set_translator('html', HTMLTranslator)

# Finds the href part of a header.
HREF = re.compile('href="([^"]+)"')

class HTMLTranslator(SphinxHTMLTranslator):

    # Removes the paragraph icon, and makes links clickable.
    def depart_title(self, node):
        SphinxHTMLTranslator.depart_title(self, node)
        # The second to last element added contains a "headerlink" class.
        # See https://bitbucket.org/birkenfeld/sphinx/src/72dceb35264e9429c8d9bb1a249a638ac21f0524/sphinx/writers/html.py#lines-501
        if len(self.body) > 2 and "headerlink" in self.body[-2]:
            match = HREF.search(self.body[-2])
            if match:
                link = match.group(1)
                # If header link was injected and the href could be
                # found, then change the href node to a closing </a>
                # anchor tag. Look at most at the 10 most recent nodes
                # to find where the anchor was opened.
                for i in range(-2, max(-10, -len(self.body)), -1):
                    if self.body[i].startswith("<h"):
                        self.body[i] += '<a href="' + link + '">'
                        self.body[-2] = "</a>"

    # Make production lists use ABNF and not BNF notation.
    def visit_productionlist(self, node):
        # type: (nodes.Node) -> None
        self.body.append(self.starttag(node, 'pre'))
        lastname = None
        for production in node:
            if production['tokenname']:
                if lastname is not None:
                    self.body.append('\n')
                lastname = production['tokenname']
                self.body.append(self.starttag(production, 'strong', ''))
                self.body.append(lastname + "</strong> =\n    ")
            elif lastname is not None:
                self.body.append('  ')
            production.walkabout(self)
            self.body.append('\n')
        self.body.append('</pre>\n')
        raise nodes.SkipNode
