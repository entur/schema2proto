package no.entur.schema2proto;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Schema2ProtoConfiguration {
	public File xsdFile = null;
	public String outputFilename = "schema2proto.proto";
	public File outputDirectory = null;
	public String defaultProtoPackage = null;
	public String forceProtoPackage = null;
	// public boolean splitBySchema = false;
	public Map<Pattern, String> customTypeMappings = new HashMap<>();;
	public Map<Pattern, String> customNameMappings = new HashMap<>();;
	public List<String> customImports = new ArrayList<>();
	public Map<String, String> options = new HashMap<>();
	// public boolean nestEnums = false;
	// public boolean typeInEnums = true;
	public boolean includeMessageDocs = true;
	public boolean includeFieldDocs = true;
	public boolean includeSourceLocationInDoc = false;
}
