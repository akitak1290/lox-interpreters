# Need to pass in ROOT, INTERPRETER, and TEST_TYPE
# Currently mixing in bash commands, might get confusing

TEST_DIR := $(ROOT)/test/$(TEST_TYPE)
TEST_FILES := $(wildcard $(TEST_DIR)/*.test.lox)
ACTION_LIST := $(patsubst $(TEST_DIR)/%.test.lox, $(TEST_DIR)/%, $(TEST_FILES))
TMP_FILE := ./cnt.tmp

total_tests := $(words $(TEST_FILES))

test: prep $(ACTION_LIST)
	@ passed=$$(wc -l < $(TMP_FILE)); \
	rm $(TMP_FILE); \
	echo "$$passed/$(total_tests) tests passed"; \
	if [ $$passed -lt $(total_tests) ]; then \
		exit 1; \
	fi

prep:
	@ touch $(TMP_FILE)

# Some tests need to exit with an error
# For those, we compare the error with .result
# and tell make to continue running with '-'
$(ACTION_LIST):
	@ $(ROOT)/$(INTERPRETER) $@.test.lox --debug $(TEST_TYPE) >$@.tmp 2>&1 || true
	@ if diff -q $@.result.lox $@.tmp > /dev/null; then \
		echo "Test $@: PASSED"; \
		echo "1" >> $(TMP_FILE); \
		rm $@.tmp; \
	else \
		echo "Test $@: FAILED"; \
		diff $@.result.lox $@.tmp; \
		rm $@.tmp; \
	fi

.PHONY: test prep $(ACTION_LIST)
