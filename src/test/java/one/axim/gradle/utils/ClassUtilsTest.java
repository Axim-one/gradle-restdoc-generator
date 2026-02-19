package one.axim.gradle.utils;

import org.gradle.api.tasks.SourceSet;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClassUtilsTest {
	@Test
	public void getMainSourceSet() {
		ClassUtils c = new ClassUtils(null);
		SourceSet expected = null;
		SourceSet actual = c.getMainSourceSet();

		assertEquals(expected, actual);
	}
}
