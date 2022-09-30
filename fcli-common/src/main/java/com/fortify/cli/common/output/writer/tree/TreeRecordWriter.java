/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.RecordWriterConfig;

import hu.webarticum.treeprinter.SimpleTreeNode;
//import hu.webarticum.treeprinter.ListingTreePrinter;
import hu.webarticum.treeprinter.printer.listing.ListingTreePrinter;

// TODO Use PrintWriter from RecordWriterConfig, wo allow output to file
public class TreeRecordWriter extends AbstractFormattedRecordWriter {
    public TreeRecordWriter(RecordWriterConfig config) {
        super(config);
    }

    @Override
    public void writeFormattedRecord(ObjectNode record) {
        SimpleTreeNode rootNode = new SimpleTreeNode("-+-");
        treeBuilder(rootNode, record, null);
        ListingTreePrinter.builder().ascii().build().print(rootNode); // TODO print to actual output, but for some reason line below doesn't work
        //ListingTreePrinter.createBuilder().ascii().build().print(rootNode, config.getPrintWriterSupplier().get());  // print with ascii
        //new ListingTreePrinter().print(rootNode); // print with unicode
    }

    private static void treeBuilder(SimpleTreeNode treeNode, JsonNode inputNode, String firstLevelLabelSuffix){
        firstLevelLabelSuffix = (firstLevelLabelSuffix == null) ? "" : firstLevelLabelSuffix;
        if( inputNode.getNodeType() == JsonNodeType.ARRAY){
            int cnt = 0;
            for (JsonNode n: inputNode) {
                SimpleTreeNode childNode = new SimpleTreeNode(String.format("#%d:%s", cnt, firstLevelLabelSuffix) );
                treeBuilder(childNode, n, null);
                treeNode.addChild(childNode);
                cnt++;
            }
        }else if(inputNode.getNodeType() == JsonNodeType.OBJECT){
            for (Iterator<Map.Entry<String, JsonNode>> it = inputNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> n = it.next();
                if(n.getValue().getNodeType() != JsonNodeType.OBJECT){
                    SimpleTreeNode childNode = new SimpleTreeNode( n.getKey() + ": " + n.getValue().asText());
                    treeNode.addChild(childNode);
                }else if(n.getValue().getNodeType() == JsonNodeType.OBJECT){
                    SimpleTreeNode childNode = new SimpleTreeNode(n.getKey());
                    treeBuilder(childNode, n.getValue(), null);
                    treeNode.addChild(childNode);
                }
            }
        } else if(inputNode.getNodeType() == JsonNodeType.NUMBER || inputNode.getNodeType() == JsonNodeType.STRING ) {
            SimpleTreeNode childNode = new SimpleTreeNode( inputNode.asText());
            treeNode.addChild(childNode);
        }

    }

}
