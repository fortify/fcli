package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.FcliResult
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

// TODO See comments in OutputOptionsGetSpec
@Prefix("core.output.list")
class OutputOptionsListSpec extends FcliBaseSpec {
    private static final FcliResult generate(String outputFormat) {
        def args = "util sample-data list"
        if ( outputFormat!=null ) { args+=" -o "+outputFormat.replace(" ", "\\ ") } 
        return Fcli.run(args)
    }
    
    def "table.no-opts"(String outputArg) {
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==23329
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[23328].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
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
                it[0].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[23327].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "csv.no-opts"() {
        def outputArg = "csv"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==23329
                it[0] == 'id,stringValue,longValue,doubleValue,booleanValue,dateValue,dateTimeValue,nestedObjectStringValue,nestedObjectBooleanValue,nestedStringArray'
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