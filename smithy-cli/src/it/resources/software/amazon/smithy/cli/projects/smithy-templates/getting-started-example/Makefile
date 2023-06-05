smithyVersion = 1.31.0

clean:
	rm -rf weather-service/build/

build: clean
	@echo Building getting-started-example...
	(cd weather-service; smithy build)
	@echo getting-started-example built successfully

test: build
