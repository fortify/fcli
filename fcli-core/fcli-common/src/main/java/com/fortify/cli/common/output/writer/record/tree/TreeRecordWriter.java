/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.writer.record.tree;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

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
                treeBuilder(treeNode, n, null);
            }
        }else if(inputNode.getNodeType() == JsonNodeType.OBJECT){
            for (Iterator<Map.Entry<String, JsonNode>> it = inputNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> n = it.next();
                if(n.getValue().getNodeType() != JsonNodeType.OBJECT && n.getValue().getNodeType() != JsonNodeType.ARRAY){
                    SimpleTreeNode childNode = new SimpleTreeNode( n.getKey() + ": " + n.getValue().asText());
                    treeNode.addChild(childNode);
                }else if(n.getValue().getNodeType() == JsonNodeType.OBJECT || n.getValue().getNodeType() == JsonNodeType.ARRAY){
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
