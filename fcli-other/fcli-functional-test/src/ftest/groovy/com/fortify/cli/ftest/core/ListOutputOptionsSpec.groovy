package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.output.list")
class ListOutputOptionsSpec extends FcliBaseSpec {
    private boolean generate(String outputArg) {
        def args = ["util", "sample-data", "list"] as String[]
        if ( outputArg!=null ) { args+=["-o", outputArg] } 
        fcli args
    }
    
    def "table.no-opts"(String outputArg) {
        expect:
            generate(outputArg)
            out.lines
            verifyAll(out.lines) {
                size()==23329
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[23328].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A')
            }
        where:
            outputArg | _
            "table"   | _
            null      | _
    }
    
    def "table-plain.no-opts"() {
        expect:
            generate("table-plain")
            out.lines
            verifyAll(out.lines) {
                size()==23328
                it[0].contains('0      value1  1000                 0.7                     true   2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1  true  nestedArrayValue3, nestedArrayValue4')
                it[23327].contains('23327  N/A     N/A                  N/A                     N/A    N/A         N/A                        N/A                 N/A   N/A')
            }
    }
    
    def "csv.no-opts"() {
        expect:
            generate("csv")
            out.lines
            verifyAll(out.lines) {
                size()==23329
                it[0] == 'id,stringValue,longValue,doubleValue,booleanValue,dateValue,dateTimeValue,nestedObjectStringValue,nestedObjectBooleanValue,nestedStringAray'
                it[1] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
                it[23328] == '23327,,,,,,,,,'
            }
    }
    
    def "csv-plain.no-opts"() {
        expect:
            generate("csv-plain")
            out.lines
            verifyAll(out.lines) {
                size()==23328
                it[0] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
                it[23327] == '23327,,,,,,,,,'
            }
    }
    
    def "json.no-opts"() {
        expect:
            generate("json")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "json-flat.no-opts"() {
        expect:
            generate("json-flat")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "tree.no-opts"() {
        expect:
            generate("tree")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "tree-flat.no-opts"() {
        expect:
            generate("tree-flat")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "xml.no-opts"() {
        expect:
            generate("xml")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "xml-flat.no-opts"() {
        expect:
            generate("xml-flat")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "yaml.no-opts"() {
        expect:
            generate("yaml")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "yaml-flat.no-opts"() {
        expect:
            generate("yaml-flat")
            out.lines
            verifyAll(out.lines) {
                // TODO Add expectations
            }
    }
    
    def "expr"() {
        expect:
            generate("expr={id}: {stringValue}\n")
            out.lines
            verifyAll(out.lines) {
                size()==23328
                it[0] == '0: value1'
                it[23327] == '23327: '
            }
    }
    
    def "json-properties"() {
        expect:
            generate("json-properties")
            out.lines
            verifyAll(out.lines) {
                size()==29
                // TODO Add expectations
            }
    }
}