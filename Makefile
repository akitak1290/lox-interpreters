# if then else is a shell command

TEST_FOLDERS := scanning parsing evaluating interpreting

all: jlox

jlox:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

test: $(TEST_FOLDERS)
	
$(TEST_FOLDERS):
	@ $(MAKE) -f util/test.make ROOT=. INTERPRETER=jlox TEST_TYPE=$@	

clean:
	@ $(MAKE) -f util/java.make clean

# testsuite1:

.PHONY: jlox test hello
