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
package com.fortify.cli.common.output.writer.tree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.IOutputWriter;
import com.fortify.cli.common.output.writer.OutputWriterConfig;

import hu.webarticum.treeprinter.ListingTreePrinter;
import hu.webarticum.treeprinter.SimpleTreeNode;

public class TreeOutputWriter implements IOutputWriter {

	public TreeOutputWriter(OutputWriterConfig config) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void write(JsonNode jsonNode) {
		SimpleTreeNode rootNode = new SimpleTreeNode("I'm the root!");
        rootNode.addChild(new SimpleTreeNode("I'm a child..."));
        rootNode.addChild(new SimpleTreeNode("I'm an other child..."));



        new ListingTreePrinter().print(rootNode);

        System.out.println("Not yet implemented.");
	}

}
