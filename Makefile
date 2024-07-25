# Invoke everything in one shell stop make from complaining
# about the way logger is implemented and used...
.ONESHELL:

# Pass FLAG=verbose to get debug info on what tests are run.

TEST_PATHS := $(wildcard ./test/*)
TEST_FOLDERS := $(foreach path, $(TEST_PATHS), $(notdir $(path)))
TMP_FILE := ./err.tmp

define logger
	@ if [ "$(FLAG)" = "verbose" ]; then \
		echo "$(1)"; \
	fi
endef

all: jlox

jlox:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

test: prep $(TEST_FOLDERS)
	@ errors=$$(wc -l < $(TMP_FILE)); \
	rm $(TMP_FILE); \
	if [ $$errors -gt 0 ]; then \
		echo "[error] Some tests FAILED, check output using with FLAG=verbose"; \
		exit 1; \
	else \
		echo "[info] All tests PASSED"; \
	fi

prep:
	@ echo "[info] ...try FLAG=verbose for test details..."
	@ echo "[info] Running test suite..."

$(TEST_FOLDERS):
	$(call logger,"[info] ----Running tests for '$@'...")
	@ $(MAKE) --no-print-directory -f util/test.make FLAG=$(FLAG) ROOT=. INTERPRETER=jlox TEST_TYPE=$@ 2>>$(TMP_FILE) || true

clean:
	@ $(MAKE) -f util/java.make clean

.PHONY: jlox prep test clean
