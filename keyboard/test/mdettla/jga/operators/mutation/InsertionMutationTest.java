package mdettla.jga.operators.mutation;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import mdettla.jga.core.Sequence;
import mdettla.jga.core.Specimen;

import org.junit.Test;

public class InsertionMutationTest {

	@Test
	public void testMutate() {
		// prepare
		Specimen specimen = new Sequence(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
		InsertionMutation mutation = new InsertionMutation();
		// test
		mutation.mutate(specimen, 3, 6);
		// verify
		assertEquals(new Sequence(Arrays.asList(1, 2, 3, 5, 6, 7, 4, 8)), specimen);
	}
}
