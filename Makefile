# if then else is a shell command

all: jlox

jlox:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

test:
	@ $(MAKE) -f util/test.make ROOT=. INTERPRETER=jlox TEST_TYPE=scanning	

clean:
	@ $(MAKE) -f util/java.make clean

# testsuite1:

.PHONY: jlox test hello
