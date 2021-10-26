<<<<<<< HEAD
=======
/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
>>>>>>> a07c2bb (wip: filter ouput using xpath or jsonpath)
package com.fortify.cli.common.output.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.output.writer.IOutputWriter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
=======
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.output.writer.IOutputWriter;
>>>>>>> a07c2bb (wip: filter ouput using xpath or jsonpath)
import lombok.SneakyThrows;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
<<<<<<< HEAD
import java.util.List;
=======
>>>>>>> a07c2bb (wip: filter ouput using xpath or jsonpath)

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
