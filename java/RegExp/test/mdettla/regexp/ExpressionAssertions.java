package mdettla.regexp;

import static org.junit.Assert.assertEquals;

class ExpressionAssertions {

	public static void assertMatch(Expression expression, String string, int expectedEndPosition) {
		assertMatch(expression, string, expectedEndPosition, true);
	}

	public static void assertNoMatch(Expression expression, String string, int expectedEndPosition) {
		assertMatch(expression, string, expectedEndPosition, false);
	}

	private static void assertMatch(Expression expression, String string, int expectedEndPosition, boolean isMatch) {
		CharIterator chars = new CharIterator(string);
		String message = "\"" + string + "\" should " + (isMatch ? "" : "not") + " match";
		assertEquals(message, isMatch, expression.match(chars));
		assertEquals(expectedEndPosition, chars.getPosition());
	}
}
