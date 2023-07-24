package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.FcliResult
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

// TODO Add/improve tests, and reduce duplication between OutputOptionsGetSpec/OutputOptionsListSpec. 
// Some ideas, in particular for table/csv-based tests:
// - Check for array of expected words on each line, i.e. splitting the input on spaces/comma's
// - Define expected array of words for headers and particular records in fields, i.e. 
//   expectedWordsRecordId0, expectedWordsLastRecord, expectedWordsTableHeader, expectedWordsCsvHeader
// - Define methods for performing certain checks, i.e. hasAllWords(line, expectedWords), ... 
// - Share all of the above between List/GetSpecs, for example through helper class, abstract base class,
//   or combining both get and list tests in single spec? 
@Prefix("core.output.get")
class OutputOptionsGetSpec extends FcliBaseSpec {
    private static final FcliResult generate(String outputFormat) {
        def args = "util sample-data get 0"
        if ( outputFormat!=null ) { args+=" -o "+outputFormat.replace(" ", "\\ ") } 
        return Fcli.run(args)
    }
    
    def "table.no-opts"() {
        when:
            def result = generate("table")
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].contains('Id  String value  Long value  Double value  Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0   value1        1000        0.7           true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
            }
    }
    
    def "table-plain.no-opts"() {
        def outputArg = "table-plain"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0].contains('0  value1  1000  0.7  true  2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1  true  nestedArrayValue3, nestedArrayValue4')
            }
    }
    
    def "csv.no-opts"() {
        def outputArg = "csv"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0] == 'id,stringValue,longValue,doubleValue,booleanValue,dateValue,dateTimeValue,nestedObjectStringValue,nestedObjectBooleanValue,nestedStringAray'
                it[1] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
            }
    }
    
    def "csv-plain.no-opts"() {
        def outputArg = "csv-plain"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
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
    
    def "yaml.no-opts"(String outputArg) {
        // For now, Yaml is the default output for get operations; if this is ever 
        // changed (see comment at StandardOutputConfig#details), the where-block
        // below will need to be moved to the appropriate method in this spec.
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
        where: 
            outputArg | _
            "yaml"    | _
            null      | _
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
        def outputArg = "expr={id}: {stringValue}"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==1
                it[0] == '0: value1'
            }
    }
    
    def "json-properties"() {
        def outputArg = "json-properties"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==20
                // TODO Add expectations
            }
    }
}