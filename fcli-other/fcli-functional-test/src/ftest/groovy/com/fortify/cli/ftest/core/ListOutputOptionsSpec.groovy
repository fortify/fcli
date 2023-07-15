package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.FcliResult
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.output.list")
class ListOutputOptionsSpec extends FcliBaseSpec {
    private FcliResult generate(String outputArg) {
        def args = ["util", "sample-data", "list"] as String[]
        if ( outputArg!=null ) { args+=["-o", outputArg] } 
        return Fcli.runOrFail(args)
    }
    
    def "table.no-opts"(String outputArg) {
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
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
        def outputArg = "table-plain"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==23328
                it[0].contains('0      value1  1000                 0.7                     true   2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1  true  nestedArrayValue3, nestedArrayValue4')
                it[23327].contains('23327  N/A     N/A                  N/A                     N/A    N/A         N/A                        N/A                 N/A   N/A')
            }
    }
    
    def "csv.no-opts"() {
        def outputArg = "csv"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==23329
                it[0] == 'id,stringValue,longValue,doubleValue,booleanValue,dateValue,dateTimeValue,nestedObjectStringValue,nestedObjectBooleanValue,nestedStringAray'
                it[1] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
                it[23328] == '23327,,,,,,,,,'
            }
    }
    
    def "csv-plain.no-opts"() {
        def outputArg = "csv-plain"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==23328
                it[0] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
                it[23327] == '23327,,,,,,,,,'
            }
    }
    
    def "json.no-opts"() {
        def outputArg = "json"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "json-flat.no-opts"() {
        def outputArg = "json-flat"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "tree.no-opts"() {
        def outputArg = "tree"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "tree-flat.no-opts"() {
        def outputArg = "tree-flat"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "xml.no-opts"() {
        def outputArg = "xml"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "xml-flat.no-opts"() {
        def outputArg = "xml-flat"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "yaml.no-opts"() {
        def outputArg = "yaml"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "yaml-flat.no-opts"() {
        def outputArg = "table-plain"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "expr"() {
        def outputArg = "expr={id}: {stringValue}\n"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==23328
                it[0] == '0: value1'
                it[23327] == '23327: '
            }
    }
    
    def "json-properties"() {
        def outputArg = "json-properties"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==29
                // TODO Add expectations
            }
    }
}