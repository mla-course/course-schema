.PHONY: repl test test-watch lint fmt fmt-fix outdated build clean

repl:        ## Start an nREPL-connected REPL
	clj -M:repl

test:        ## Run the test suite once
	clj -X:test

test-watch:  ## Re-run tests on file change
	clj -M:test --watch

lint:        ## Static analysis (clj-kondo)
	clj-kondo --lint src test

fmt:         ## Check formatting (cljfmt)
	clj -M:fmt check

fmt-fix:     ## Rewrite files to canonical formatting
	clj -M:fmt fix

outdated:    ## List outdated dependencies (antq)
	clj -M:outdated

build:       ## Build an uberjar
	clj -T:build uber

clean:       ## Remove build artifacts
	rm -rf target
