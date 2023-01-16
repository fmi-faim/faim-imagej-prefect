package ch.fmi.prefect;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.module.DefaultMutableModuleInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.module.MutableModuleItem;

public class FlowParameterTest {

	//private static Gson gson;
	private static Context context;
	private static Module module;
	private static JSONObject parameter_openapi_schema;
	private static JSONObject properties;
	private static JSONObject definitions;
	private static JSONObject parameters;

	@BeforeClass
	public static void setUpBeforeClass() throws JSONException, IOException {
		//gson = new Gson();
		context = new Context(ModuleService.class);
		module = context.service(ModuleService.class).createModule(new DefaultMutableModuleInfo());
		parameter_openapi_schema = new JSONObject(IOUtils.toString(FlowParameterTest.class.getResourceAsStream("/parameter_openapi_schema.json"), "UTF-8"));
		properties = new JSONObject(IOUtils.toString(FlowParameterTest.class.getResourceAsStream("/properties.json"), "UTF-8"));
		definitions = new JSONObject(IOUtils.toString(FlowParameterTest.class.getResourceAsStream("/definitions.json"), "UTF-8"));
		parameters = new JSONObject(IOUtils.toString(FlowParameterTest.class.getResourceAsStream("/parameters.json"), "UTF-8"));
	}

	@AfterClass
	public static void tearDownAfterClass() {
		context.dispose();
	}

	@Test
	public void testInteger() {
//		Parameter param = new Parameter("integer", "myInteger", 42, 0);
//		String expected = "{`type`:`integer`,`title`:`myInteger`,`defaultValue`:42,`position`:0}".replace('`', '"');
//		String jsonString = gson.toJson(param);
//		assertEquals(expected, jsonString);
//		// TODO test getModuleItem(json) has type Integer
	}

	@Test
	public void testNumber() {
		// TODO
	}

	@Test
	public void testBoolean() {
		// TODO
	}

	@Test
	public void testString() {
		JSONObject stringParam = properties.getJSONObject("string");
		MutableModuleItem<?> moduleItem = ParameterUtils.getModuleItem(module, stringParam, definitions, parameters);
		assertEquals(String.class, moduleItem.getType());
		assertEquals("foo", moduleItem.getDefaultValue());
		assertFalse(moduleItem.isRequired()); // TODO required?
	}

	@Test
	public void testPath() {
//		Parameter param = new Parameter("string", "myPath", "path", "/path/to/nowhere", 3);
//		String expected = "{`type`:`string`,`title`:`myPath`,`format`:`path`,`defaultValue`:`/path/to/nowhere`,`position`:3}".replace('`', '"');
//		String jsonString = gson.toJson(param);
//		assertEquals(expected, jsonString);
//		// TODO test getModuleItem(json) has type File
	}

	@Test
	public void testParameterOrder() {
		String[] expected = {"group", "integer", "floating", "string", "path", "pathlist", "boolean", "literal", "enum"};
		List<String> sortedParameters = ParameterUtils.sorted(properties);
		assertArrayEquals(expected, sortedParameters.toArray());
	}

	@SuppressWarnings("unused")
	private class Parameter {

		private String type;
		private String title;
		private String format;
		private Object defaultValue;
		private Integer position;

		Parameter(String type, String title, Object defaultValue, int position) {
			this.type = type;
			this.title = title;
			this.defaultValue = defaultValue;
			this.position = position;
		}

		Parameter(String type, String title, String format, Object defaultValue, int position) {
			this(type, title, defaultValue, position);
			this.format = format;
		}

	}
}



