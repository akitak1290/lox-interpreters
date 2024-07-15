TEST_FOLDERS := scanning parsing evaluating assignment if
TMP_FILE := ./err.tmp

all: jlox

jlox:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

test: $(TEST_FOLDERS)
	@ errors=$$(wc -l < $(TMP_FILE)); \
	rm $(TMP_FILE); \
	if [ $$errors -gt 0 ]; then \
		echo "--Some tests failed, check output--"; \
		exit 1; \
	else \
		echo "--All tests PASSED--"; \
	fi

$(TEST_FOLDERS):
	@ echo "--Running tests for $@ function...--"
	@ $(MAKE) --no-print-directory -f util/test.make ROOT=. INTERPRETER=jlox TEST_TYPE=$@ 2>>$(TMP_FILE) || true

clean:
	@ $(MAKE) -f util/java.make clean

.PHONY: jlox test clean
