package lox;

/*
 * This exception class is used
 * to exit the current stack to
 * the caller of the function
 * containing this exception.
 */
class Return extends RuntimeException {
	final Object value;

	Return(Object value) {
		// Disable RuntimeException's goodies
		super(null, null, false, false);
		this.value = value;
	}
}
