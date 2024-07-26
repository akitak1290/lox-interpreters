# Invoke everything in one shell stop make from complaining
# about the way logger is implemented and used...
.ONESHELL:

# Need to pass in ROOT, INTERPRETER, TEST_TYPE, and optionally, FLAG
# Currently mixing in bash commands, might get confusing

TEST_DIR := $(ROOT)/test/$(TEST_TYPE)
TEST_FILES := $(wildcard $(TEST_DIR)/*.test.lox)
ACTION_LIST := $(patsubst $(TEST_DIR)/%.test.lox, $(TEST_DIR)/%, $(TEST_FILES))
TMP_FILE := ./cnt.tmp

total_tests := $(words $(TEST_FILES))

define logger
	if [ "$(FLAG)" = "verbose" ]; then \
		echo "$(1)"; \
	fi
endef

# These tests target a specific component of the interpreter
# by running the --single argument
# jlox test.lox --single parser
# should have just use Ant, or Maven...
SINGLE_FLAG :=
JLOX_FLAG :=

ifeq ($(TEST_TYPE),scanner)
	JLOX_FLAG := --single
	SINGLE_FLAG := scanner
else ifeq ($(TEST_TYPE),parser)
	JLOX_FLAG := --single
	SINGLE_FLAG := parser
else ifeq ($(TEST_TYPE),evaluator)
	JLOX_FLAG := --single
	SINGLE_FLAG := evaluator
endif

test: prep $(ACTION_LIST)
	@ passed=$$(wc -l < $(TMP_FILE)); \
	rm $(TMP_FILE); \
	$(call logger,"[info] $$passed/$(total_tests) tests passed"); \
	if [ $$passed -lt $(total_tests) ]; then \
		exit 1; \
	fi

prep:
	@ touch $(TMP_FILE)

# Some tests need to exit with an error
# For those, we compare the error with .result
# and tell make to continue running with '-'
$(ACTION_LIST):
	@ $(ROOT)/$(INTERPRETER) $@.test.lox $(JLOX_FLAG) $(SINGLE_FLAG) >$@.tmp 2>&1 || true
	@ if diff -q $@.result.lox $@.tmp > /dev/null; then \
		$(call logger,"[info] Test $@: PASSED"); \
		echo "1" >> $(TMP_FILE); \
		rm $@.tmp; \
	else \
		echo "[error] Test $@: FAILED"; \
		diff $@.result.lox $@.tmp; \
		rm $@.tmp; \
	fi

.PHONY: test prep $(ACTION_LIST)
