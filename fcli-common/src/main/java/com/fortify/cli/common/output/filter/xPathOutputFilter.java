package com.fortify.cli.common.output.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.output.writer.IOutputWriter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.List;

public class xPathOutputFilter implements IOutputFilter {

	@Override
	@SneakyThrows
	public JsonNode filter(JsonNode jsonNode, String expression) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		XmlMapper xmlMapper = new XmlMapper();

//		if(! (jsonNode instanceof ObjectNode)){
//			ObjectMapper objectMapper = new ObjectMapper();
//			ObjectNode root = objectMapper.createObjectNode();
//			root.set("content", jsonNode);
//
//			jsonNode = root;
//		}

		//System.out.println(JsonPath.parse(jsonNode.toString()).read(expression, String.class));
//		String jsonString = "{\"delivery_codes\": [{\"postal_code\": {\"district\": \"Ghaziabad\", \"pin\": 201001, \"pre_paid\": \"Y\", \"cash\": \"Y\", \"pickup\": \"Y\", \"repl\": \"N\", \"cod\": \"Y\", \"is_oda\": \"N\", \"sort_code\": \"GB\", \"state_code\": \"UP\"}}]}";
//		String jsonExp = "$.delivery_codes";
//		JsonNode pincodes = JsonPath.parse(jsonString).read(jsonExp, JsonNode.class);
//		System.out.println("pincodesJson : "+pincodes);
//
//		System.out.println(jsonNode.get(0).toPrettyString());
//
//		return jsonNode;

//		String xmlString = xmlMapper.writeValueAsString(jsonNode).replace("ObjectNode", "content");
//		InputSource xmlSource = new InputSource(new StringReader(xmlString));
//
//		ObjectNode nodelist = (ObjectNode) xPath.compile(expression).evaluate(xmlSource, XPathConstants.NODESET);
//
//		System.out.println(nodelist);
//
//		Object obj = xPath.compile(expression).evaluate(xmlSource, XPathConstants.NODESET);
//
//		System.out.println(obj);
//
//		return jsonNode ;


		DocumentContext jsonContext = JsonPath.parse(jsonNode.toString());
		List<String> output = jsonContext.read(expression);

		ObjectMapper mapper = new ObjectMapper();
		ArrayNode array = mapper.valueToTree(output);

		return array;
	}

}
