package ch.fmi.prefect;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.scijava.module.MutableModuleItem;
import org.scijava.module.DefaultMutableModuleItem;
import org.scijava.module.Module;

public class ParameterUtils {

	private ParameterUtils() {
		// prevent instantiation of static utility class
	}

	public static List<String> sorted(JSONObject properties) {
		Map<String, Integer> parameters = new LinkedHashMap<>();
		for (String name : properties.keySet()) {
			parameters.put(name, properties.getJSONObject(name).getInt("position"));
		}
		return parameters.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(e -> e.getKey()).collect(Collectors.toList());
	}

	public static <T> MutableModuleItem<T> getModuleItem(Module module, JSONObject param, JSONObject definitions, JSONObject parameters) {
		Class<T> type = getWidgetType(param.optString("type"), param.optString("format"), param.optJSONObject("items"));
		DefaultMutableModuleItem<T> moduleItem = new DefaultMutableModuleItem<>(module, param.getString("title"), type);
		
		return moduleItem;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> getWidgetType(String type, String format, JSONObject items) {
		if (type != null) {
			switch (type) {
			case "array":
				// special-casing lists of path-like objects => FileListWidget 
				if (items.optString("type").equals("string") && items.optString("format").equals("path")) {
					return (Class<T>) File[].class;
				}
				// fallback to String[]
				// TODO improve once there's a usable List<?> widget in SciJava
				return (Class<T>) String[].class;
			case "number":
				return (Class<T>) Double.class;
			case "integer":
				return (Class<T>) Integer.class;
			case "boolean":
				return (Class<T>) Boolean.class;
			case "string":
				if (format.equals("path")) {
					return (Class<T>) File.class;
				}
			default:
				return (Class<T>) String.class;
			}
		} else {
			// type is null, so we look for enum, allOf, anyOf.
			return (Class<T>) String.class; // fallback
		}
	}
}
