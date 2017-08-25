package io.swagger.codegen.languages;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class KongGatewayCodegen extends DefaultCodegen implements CodegenConfig {

	private static final String KONG_HOST = "kongHost";
	private static final String KONG_PORT = "kongPort";
	private static final String TARGET_API_NAME = "targetApiName";

	protected String sourceFolder = "";
	protected String apiVersion = "1.0.0";
	protected String kongHost = "localhost";
	protected String kongPort = "8001";
	protected String targetApiName = "myApi";

	/**
	 * Configures the type of generator.
	 * 
	 * @return the CodegenType for this generator
	 * @see io.swagger.codegen.CodegenType
	 */
	@Override
	public CodegenType getTag() {
		return CodegenType.DOCUMENTATION;
	}

	/**
	 * Configures a friendly name for the generator. This will be used by the generator to select the
	 * library with the -l flag.
	 * 
	 * @return the friendly name for the generator
	 */
	@Override
	public String getName() {
		return "kong";
	}

	/**
	 * Returns human-friendly help for the generator. Provide the consumer with help tips, parameters
	 * here
	 * 
	 * @return A string value for the help message
	 */
	@Override
	public String getHelp() {
		return "Generates a bash-script to create routes to a kong gateway.";
	}

	public KongGatewayCodegen() {
		super();

		// set the output folder here
		outputFolder = "generated-code/kongCodegen";

		/*
		 * Api classes. You can write classes for each Api file with the apiTemplateFiles map. as with
		 * models, add multiple entries with different extensions for multiple files per class
		 */
		apiTemplateFiles.put("kong-bash-script.mustache", // the template to use
				".sh"); // the extension for each file to write

		/*
		 * Template Location. This is the location which templates will be read from. The generator will use
		 * the resource stream to attempt to read the templates.
		 */
		embeddedTemplateDir = templateDir = "kong";

		/*
		 * Api Package. Optional, if needed, this can be used in templates
		 */
		apiPackage = "";

		/*
		 * Model Package. Optional, if needed, this can be used in templates
		 */
		modelPackage = "";

		/*
		 * Reserved words. Override this with reserved words specific to your language
		 */
		reservedWords = new HashSet<String>(Arrays.asList("sample1", // replace with static values
				"sample2"));

		/*
		 * Additional Properties. These values can be passed to the templates and are available in models,
		 * apis, and supporting files
		 */
		additionalProperties.put("apiVersion", apiVersion);

		cliOptions.add(CliOption.newString(KONG_HOST, "The host where kong API gateway is running on."));
		cliOptions.add(CliOption.newString(KONG_PORT, "The port where kong API gateway is running on."));
		cliOptions.add(CliOption.newString(TARGET_API_NAME, "The API name that should be registered in kong."));

	}

	@Override
	public void preprocessSwagger(Swagger swagger) {
		if (swagger != null && swagger.getPaths() != null) {
			for (String pathname : swagger.getPaths().keySet()) {
				Path path = swagger.getPath(pathname);
				if (path.getOperations() != null) {
					for (Operation operation : path.getOperations()) {
						String pathWithDollars = pathname.replaceAll("\\{", "\\$\\{");
						operation.setVendorExtension("x-path", pathWithDollars);
					}
				}
			}
		}
	}

	/**
	 * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping those terms
	 * here. This logic is only called if a variable matches the reseved words
	 * 
	 * @return the escaped term
	 */
	@Override
	public String escapeReservedWord(String name) {
		if (this.reservedWordsMappings().containsKey(name)) {
			return this.reservedWordsMappings().get(name);
		}
		return "_" + name;
	}

	/**
	 * Location to write model files. You can use the modelPackage() as defined when the class is
	 * instantiated
	 */
	@Override
	public String modelFileFolder() {
		return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
	}

	/**
	 * Location to write api files. You can use the apiPackage() as defined when the class is
	 * instantiated
	 */
	@Override
	public String apiFileFolder() {
		return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
	}

	/**
	 * Optional - type declaration. This is a String which is used by the templates to instantiate your
	 * types. There is typically special handling for different property types
	 *
	 * @return a string value used as the `dataType` field for model templates, `returnType` for api
	 *         templates
	 */
	@Override
	public String getTypeDeclaration(Property p) {
		if (p instanceof ArrayProperty) {
			ArrayProperty ap = (ArrayProperty) p;
			Property inner = ap.getItems();
			return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
		} else if (p instanceof MapProperty) {
			MapProperty mp = (MapProperty) p;
			Property inner = mp.getAdditionalProperties();
			return getSwaggerType(p) + "[String, " + getTypeDeclaration(inner) + "]";
		}
		return super.getTypeDeclaration(p);
	}

	@Override
	public void processOpts() {
		super.processOpts();

		if (additionalProperties.containsKey(KONG_HOST)) {
			setKongHost((String) additionalProperties.get(KONG_HOST));
		}
		additionalProperties.put(KONG_HOST, kongHost);

		if (additionalProperties.containsKey(KONG_PORT)) {
			setKongPort((String) additionalProperties.get(KONG_PORT));
		}
		additionalProperties.put(KONG_PORT, kongPort);

		if (additionalProperties.containsKey(TARGET_API_NAME)) {
			setTargetApiName((String) additionalProperties.get(TARGET_API_NAME));
		}
		additionalProperties.put(TARGET_API_NAME, targetApiName);
	}

	/**
	 * Optional - swagger type conversion. This is used to map swagger types in a `Property` into either
	 * language specific types via `typeMapping` or into complex models if there is not a mapping.
	 *
	 * @return a string value of the type or complex model for this property
	 * @see io.swagger.models.properties.Property
	 */
	@Override
	public String getSwaggerType(Property p) {
		String swaggerType = super.getSwaggerType(p);
		String type = null;
		if (typeMapping.containsKey(swaggerType)) {
			type = typeMapping.get(swaggerType);
			if (languageSpecificPrimitives.contains(type))
				return toModelName(type);
		} else
			type = swaggerType;
		return toModelName(type);
	}

	@Override
	public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Model> definitions) {
		path = replacePathParam(path);
		return super.fromOperation(path, httpMethod, operation, definitions, null);
	}

	private String replacePathParam(String path) {
		if (path.contains("{") && path.contains("}")) {
			int begin = path.indexOf("{");
			int end = path.indexOf("}");
			String parameterName = path.subSequence(begin, end).toString();
			path = path.replaceFirst(begin + parameterName + end, "$(uri_captures." + parameterName + ")");
		}

		if (path.contains("{") && path.contains("}")) {
			path = replacePathParam(path);
		}

		return path;
	}

	@Override
	public String escapeQuotationMark(String input) {
		return input.replace("'", "");
	}

	@Override
	public String escapeUnsafeCharacters(String input) {
		return input.replace("*/", "*_/").replace("/*", "/_*");
	}

	public void setKongHost(String kongHost) {
		this.kongHost = kongHost;
	}

	public void setKongPort(String kongPort) {
		this.kongPort = kongPort;
	}

	public void setTargetApiName(String targetApiName) {
		this.targetApiName = targetApiName;
	}

}
