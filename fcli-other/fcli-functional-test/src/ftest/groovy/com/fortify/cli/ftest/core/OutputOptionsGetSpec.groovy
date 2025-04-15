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
// TODO Add/update tests for new fcli 3.x --style option
@Prefix("core.output.get")
class OutputOptionsGetSpec extends FcliBaseSpec {
    private static final FcliResult generate(String outputFormat) {
        def args = "util sample-data get 0"
        if ( outputFormat!=null ) { args+=" -o "+outputFormat.replace(" ", "\\ ") } 
        return Fcli.run(args)
    }
    
    def "table"() {
        when:
            def result = generate("table")
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
            }
    }
    
    def "csv"() {
        def outputArg = "csv"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0] == 'id,stringValue,longValue,doubleValue,booleanValue,dateValue,dateTimeValue,nestedObject.stringValue,"nestedObject.booleanValue",nestedStringArray'
                it[1] == '0,value1,1000,0.7,true,2000-01-01,"2000-01-01T00:00:00+00:00",nestedObjectValue1,true,"nestedArrayValue3, nestedArrayValue4"'
            }
    }
    
    def "json"() {
        def outputArg = "json"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "xml"() {
        def outputArg = "xml"
        when:
            def result = generate(outputArg)
        then:
            verifyAll(result.stdout) {
                // TODO Add expectations
            }
    }
    
    def "yaml"(String outputArg) {
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
}