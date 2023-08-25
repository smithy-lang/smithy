# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = python3 -msphinx
SPHINXPROJ    = Smithy

install:
	pip3 install -r requirements.txt .
	pip3 install -e .

clean:
	-rm -rf build/*

html1:
	@$(SPHINXBUILD) -M html "source-1.0" "build/1.0" $(SPHINXOPTS) -W --keep-going -n $(O)

html2:
	@$(SPHINXBUILD) -M html "source-2.0" "build/2.0" $(SPHINXOPTS) -W --keep-going -n $(O)

html: html1 html2 merge-versions

merge-versions:
	mkdir -p build/html
	cp -R "build/1.0/html" "build/html/1.0"
	cp -R "build/2.0/html" "build/html/2.0"
	cp -R "root/" "build/html"

openhtml:
	open "build/html/index.html"

.PHONY: Makefile clean
