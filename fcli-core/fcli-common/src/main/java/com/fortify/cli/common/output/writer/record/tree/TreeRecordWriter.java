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
import hu.webarticum.treeprinter.printer.listing.ListingTreePrinter;

// TODO Use PrintWriter from RecordWriterConfig, wo allow output to file
public class TreeRecordWriter extends AbstractFormattedRecordWriter {
    public TreeRecordWriter(RecordWriterConfig config) {
        super(config);
    }

    @Override
    public void writeFormattedRecord(ObjectNode record) {
        SimpleTreeNode rootNode = new SimpleTreeNode("─┬─");
        treeBuilder(rootNode, record);
        //We previously printed in ascii, waiting on feedback from MS if there was a specific reason for that
        //ListingTreePrinter.builder().ascii().build().print(rootNode); 
        new ListingTreePrinter().print(rootNode); // print with unicode
    }

    private static void treeBuilder(SimpleTreeNode treeNode, JsonNode inputNode){
        JsonNodeType nodeType = inputNode.getNodeType();
        
        switch(nodeType) {
        case NUMBER:
        case STRING:
        case BOOLEAN:
            addPrimitiveChildNode(treeNode, inputNode.asText());
            break;
        case OBJECT:
            addObjectChildNode(treeNode, inputNode);
            break;
        case ARRAY:
            for (JsonNode n: inputNode) {
                treeBuilder(treeNode, n);
            }
            break;
        default:
            break;
        }

    }
    
    private static void addPrimitiveChildNode(SimpleTreeNode treeNode, String text) {
        SimpleTreeNode childNode = new SimpleTreeNode( text );
        treeNode.addChild(childNode);
    }
    
    private static void addObjectChildNode(SimpleTreeNode treeNode, JsonNode inputNode) {

        for (Iterator<Map.Entry<String, JsonNode>> it = inputNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> n = it.next();
            if(n.getValue().isContainerNode()) {
                SimpleTreeNode childNode = new SimpleTreeNode(n.getKey());
                treeBuilder(childNode, n.getValue());
                treeNode.addChild(childNode);
            } else {
                addPrimitiveChildNode(treeNode, n.getKey() + ": " + n.getValue().asText());
            }
        }
    }

}
