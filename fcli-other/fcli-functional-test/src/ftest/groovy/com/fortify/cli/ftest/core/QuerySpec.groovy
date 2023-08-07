package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.FcliResult
import com.fortify.cli.ftest._common.Fcli.UnexpectedFcliResultException
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.query")
class QuerySpec extends FcliBaseSpec {
    private static final FcliResult generate(String query) {
        def args = "util sample-data list"
        if ( query!=null ) { args+=" -q "+query.replace(" ", "\\ ") }
        return Fcli.run(args)
    }
    
    def "containsInt"() {
        when:
            def result = generate("{0,23327,null}.contains(id)")
        then:
            verifyAll(result.stdout) {
                size()==3
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[2].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "containsString"() {
        when:
            def result = generate("{'value1',null}.contains(stringValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "containsLong"() {
        when:
            def result = generate("{9223372036854775807L,1000L,-2000L,null}.contains(longValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[1297].replace(" ","").equals('3888value1-20000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "containsDouble"() {
        when:
            def result = generate("{0.7,1.7976931348623157E308,null}.contains(doubleValue)")
        then:
            verifyAll(result.stdout) {
                size()==11665
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[11448].replace(" ","").equals('23111N/AN/A1.7976931348623157E308N/AN/AN/AN/AN/AN/A')
                it[11664].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "containsBool"() {
        when:
            def result = generate("{true,null}.contains(booleanValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "containsDate"() {
        when:
            def result = generate("{'2000-01-01',null}.contains(dateValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "containsDateTime"() {
        when:
            def result = generate("{'2000-01-01T00:00:00+00:00',null}.contains(dateTimeValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23327N/AN/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "equalsInt"() {
        when:
        def result = generate("id==0")
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
            }

    }
    
    def "equalsString"() {
        when:
        def result = generate("stringValue=='value1'")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('7775value1N/AN/AN/AN/AN/AN/AN/AN/A')
            }

    }
    
    def "equalsLong"() {
        when:
        def result = generate("longValue==9223372036854775807L")
        then:
            verifyAll(result.stdout) {
                size()==3889
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('5184value192233720368547758070.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[3888].replace(" ","").equals('22031N/A9223372036854775807N/AN/AN/AN/AN/AN/AN/A')
            }

    }
    
    def "equalsDouble"() {
        when:
        def result = generate("doubleValue==0.7")
        then:
            verifyAll(result.stdout) {
                size()==3889
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[3888].replace(" ","").equals('22247N/AN/A0.7N/AN/AN/AN/AN/AN/A')
            }

    }
    
    def "equalsBool"() {
        when:
        def result = generate("booleanValue==true")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('23183N/AN/AN/AtrueN/AN/AN/AN/AN/A')
            }

    }
    
    def "equalsDate"() {
        when:
        def result = generate("dateValue=='2000-01-01'")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('23279N/AN/AN/AN/A2000-01-01N/AN/AN/AN/A')
            }

    }
    
    def "equalsDatetime"() {
        when:
        def result = generate("#date(dateTimeValue)==#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('23311N/AN/AN/AN/AN/A2000-01-01T00:00:00+00:00N/AN/AN/A')
            }

    }
    
    
    def "equalsOrIntString"() {
        when:
            def result = generate("id==0 || stringValue=='value2'")
        then:
            verifyAll(result.stdout) {
                size()==7778
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7777].replace(" ","").equals('15551value2N/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "equalsOrLongDouble"() {
        when:
            def result = generate("longValue==9223372036854775807L || doubleValue==0.7")
        then:
            verifyAll(result.stdout) {
                size()==7129
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1081].replace(" ","").equals('5400value19223372036854775807-0.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7128].replace(" ","").equals('22247N/AN/A0.7N/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "equalsOrBoolDate"() {
        when:
            def result = generate("booleanValue==false || dateValue=='2000-01-01'")
        then:
            verifyAll(result.stdout) {
                size()==12961
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[49].replace(" ","").equals('96value110000.7false2030-12-312000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[12960].replace(" ","").equals('23279N/AN/AN/AN/A2000-01-01N/AN/AN/AN/A')
            }
    }
    
    def "equalsOrLongDateTime"() {
        when:
            def result = generate("longValue==9223372036854775807L || #date(dateTimeValue)==#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==10369
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1737].replace(" ","").equals('5192value192233720368547758070.7true2000-01-012030-12-31T23:59:59+02:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[10368].replace(" ","").equals('23311N/AN/AN/AN/AN/A2000-01-01T00:00:00+00:00N/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterThanInt"() {
        when:
            def result = generate("id<9999 && id>1111")
        then:
            verifyAll(result.stdout) {
                size()==8888
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('1112value11000N/Atrue2030-12-312030-12-31T23:59:59+02:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[8887].replace(" ","").equals('9998value2-10001.7976931348623157E308trueN/A2030-12-31T23:59:59+02:00N/AN/AnestedArrayValue3,nestedArrayValue4')
            }
    }
    
    def "smallerAndGreaterThanLong"() {
        when:
            def result = generate("longValue<9223372036854775807L && longValue>1111L")
        then:
            verifyAll(result.stdout) {
                size()==3889
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('2592value120000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[3888].replace(" ","").equals('19439N/A2000N/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterThanDouble"() {
        when:
            def result = generate("doubleValue<1.7976931348623157E308 && doubleValue>-0.7")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('22679N/AN/A1.4N/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterThanDateTime"() {
        when:
            def result = generate("#date(dateTimeValue)<#date('2031-12-31T23:59:59+02:00') && #date(dateTimeValue)>#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('8value110000.7true2000-01-012030-12-31T23:59:59+02:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('23319N/AN/AN/AN/AN/A2030-12-31T23:59:59+02:00N/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterEqualInt"() {
        when:
            def result = generate("id<=9999 && id>=1111")
        then:
            verifyAll(result.stdout) {
                size()==8890
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('1111value11000N/Atrue2030-12-312000-01-01T00:00:00+00:00N/AN/AN/A')
                it[8889].replace(" ","").equals('9999value2-10001.7976931348623157E308trueN/A2030-12-31T23:59:59+02:00N/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterEqualLong"() {
        when:
            def result = generate("longValue<=9223372036854775807L && longValue>=1000L")
        then:
            verifyAll(result.stdout) {
                size()==11665
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[11664].replace(" ","").equals('22031N/A9223372036854775807N/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterEqualDouble"() {
        when:
            def result = generate("doubleValue<=1.7976931348623157E308 && doubleValue>=-0.7")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23111N/AN/A1.7976931348623157E308N/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "smallerAndGreaterEqualDateTime"() {
        when:
            def result = generate("#date(dateTimeValue)<=#date('2031-12-31T23:59:59+02:00') && #date(dateTimeValue)>=#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('23319N/AN/AN/AN/AN/A2030-12-31T23:59:59+02:00N/AN/AN/A')
            }
    }
    
    
    def "smallerAndGreaterEqualDateTimeNow"() {
        when:
            def result = generate("(#date(dateTimeValue)<=#now('+14610d') && #date(dateTimeValue)>=#now('+1000d'))")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('8value110000.7true2000-01-012030-12-31T23:59:59+02:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[7776].replace(" ","").equals('23319N/AN/AN/AN/AN/A2030-12-31T23:59:59+02:00N/AN/AN/A')
            }
    }
    /*
    def "matches"() {
        when:
            def result = generate("stringValue matches 'value1|value2'")
        then:
            verifyAll(result.stdout) {
                size()==15552
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[1].contains('15551  value2        N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }*/
    
    def "matchesWithNullGuard"() {
        when:
            def result = generate("stringValue != null && stringValue matches 'value1|value2'")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].replace(" ","").equals('IdStringvalueLongvalueDoublevalueBooleanvalueDatevalueDatetimevalueNestedobjectstringvalueNestedobjectbooleanvalueNestedstringarray')
                it[1].replace(" ","").equals('0value110000.7true2000-01-012000-01-01T00:00:00+00:00nestedObjectValue1truenestedArrayValue3,nestedArrayValue4')
                it[15552].replace(" ","").equals('15551value2N/AN/AN/AN/AN/AN/AN/AN/A')
            }
    }
    
    def "throwsOnNonExistingProperty"() {
        when:
            def result = generate("nonexistingvalue == 'value1'")
        then:
            def e = thrown(UnexpectedFcliResultException)
            verifyAll(e.result.stderr) {
                it.any {it.contains('Property or field \'nonexistingvalue\' cannot be found on object of type \'com.fasterxml.jackson.databind.node.ObjectNode\' - maybe not public or not valid?')}
                it.any {it.contains('java.lang.IllegalStateException: Error evaluating query expression:')}
            }
    }
    
}
