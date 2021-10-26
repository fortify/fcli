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
package com.fortify.cli.common.util.printer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import hu.webarticum.treeprinter.ListingTreePrinter;
import hu.webarticum.treeprinter.SimpleTreeNode;


import java.util.Arrays;
import java.util.List;

import static java.lang.System.exit;

public class PrintHelper {

    public static final void printToFormat(String format, JsonNode json){
        switch(format.toLowerCase()){
            case "yaml":
                printYaml(json);
                break;
            case "table":
                printTable(json);
                break;
            case "json":
                System.out.println(json.toPrettyString());
                break;
            case "tree":
                printTree(json);
                break;
            default:
                System.out.println("INVALID FORMAT! Accepted formats are: json, table, tree, yaml.");
        }
    }

    // TODO: Actually implement code to make table output work
    public static final void printTable(JsonNode json){
        class Planet{
            int num;
            String name;
            double diameter;
            double mass;
            String atmosphere;
            public Planet(int num, String name, double diameter, double mass, String atmosphere){
                this.num = num;
                this.name = name;
                this.diameter = diameter;
                this.mass = mass;
                this.atmosphere = atmosphere;
            }
        }
        List<Planet> planets = Arrays.asList(
                new Planet(1, "Mercury", 0.382, 0.06, "minimal"),
                new Planet(2, "Venus", 0.949, 0.82, "Carbon dioxide, Nitrogen"),
                new Planet(3, "Earth", 1.0, 1.0, "Nitrogen, Oxygen, Argon"),
                new Planet(4, "Mars", 0.532, 0.11, "Carbon dioxide, Nitrogen, Argon"));

        Character[] borderStyles = AsciiTable.NO_BORDERS;
        System.out.println(AsciiTable.getTable(borderStyles, planets, Arrays.asList(
                new Column().dataAlign(HorizontalAlign.CENTER).header("#").with(planet -> Integer.toString(planet.num)),
                new Column().dataAlign(HorizontalAlign.LEFT).header("Name").with(planet -> planet.name),
                new Column().dataAlign(HorizontalAlign.CENTER).header("Diameter").with(planet -> String.format("%.03f", planet.diameter)),
                new Column().dataAlign(HorizontalAlign.CENTER).header("Mass").with(planet -> String.format("%.02f", planet.mass)),
                new Column().dataAlign(HorizontalAlign.LEFT).header("Atmosphere").with(planet -> planet.atmosphere))));

        System.out.println("Not yet implemented.");
    }

//    private SimpleTreeNode walker (ObjectNode node){
//        if(node == null){
//            return null;
//        }
//
//    }
    // TODO: Actually implement code to make tree output work
    public static final void printTree(JsonNode json){
        SimpleTreeNode rootNode = new SimpleTreeNode("I'm the root!");
        rootNode.addChild(new SimpleTreeNode("I'm a child..."));
        rootNode.addChild(new SimpleTreeNode("I'm an other child..."));



        new ListingTreePrinter().print(rootNode);

        System.out.println("Not yet implemented.");
    }

    public static final void printYaml(JsonNode json) {
        try {
            System.out.print(new YAMLMapper().writeValueAsString(json));
        } catch (JsonProcessingException e){
            System.out.println(e);
            exit(1);
        }
    }

}
