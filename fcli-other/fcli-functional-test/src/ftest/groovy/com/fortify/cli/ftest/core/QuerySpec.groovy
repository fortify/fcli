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
                it[0].contains('Id     String value  Long value  Double value  Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000        0.7           true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[2].contains('23327  N/A           N/A         N/A           N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "containsString"() {
        when:
            def result = generate("{'value1',null}.contains(stringValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A')
            }
    }
    
    def "containsLong"() {
        when:
            def result = generate("{9223372036854775807L,1000L,-2000L,null}.contains(longValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[1297].contains('3888   value1        -2000                0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A')
            }
    }
    
    def "containsDouble"() {
        when:
            def result = generate("{0.7,1.7976931348623157E308,null}.contains(doubleValue)")
        then:
            verifyAll(result.stdout) {
                size()==11665
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[11448].contains('23111  N/A           N/A                  1.7976931348623157E308  N/A            N/A         N/A                        N/A                         N/A                          N/A')
                it[11664].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A')
            }
    }
    
    def "containsBool"() {
        when:
            def result = generate("{true,null}.contains(booleanValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "containsDate"() {
        when:
            def result = generate("{'2000-01-01',null}.contains(dateValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "containsDateTime"() {
        when:
            def result = generate("{'2000-01-01T00:00:00+00:00',null}.contains(dateTimeValue)")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23327  N/A           N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "equalsInt"() {
        when:
        def result = generate("id==0")
        then:
            verifyAll(result.stdout) {
                size()==2
                it[0].contains('Id  String value  Long value  Double value  Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0   value1        1000        0.7           true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
            }

    }
    
    def "equalsString"() {
        when:
        def result = generate("stringValue=='value1'")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id    String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0     value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7776].contains('7775  value1        N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }

    }
    
    def "equalsLong"() {
        when:
        def result = generate("longValue==9223372036854775807L")
        then:
            verifyAll(result.stdout) {
                size()==3889
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('5184   value1        9223372036854775807  0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[3888].contains('22031  N/A           9223372036854775807  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }

    }
    
    def "equalsDouble"() {
        when:
        def result = generate("doubleValue==0.7")
        then:
            verifyAll(result.stdout) {
                size()==3889
                it[0].contains('Id     String value  Long value           Double value  Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7           true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[3888].contains('22247  N/A           N/A                  0.7           N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }

    }
    
    def "equalsBool"() {
        when:
        def result = generate("booleanValue==true")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7776].contains('23183  N/A           N/A                  N/A                     true           N/A         N/A                        N/A                         N/A                          N/A')
            }

    }
    
    def "equalsDate"() {
        when:
        def result = generate("dateValue=='2000-01-01'")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7776].contains('23279  N/A           N/A                  N/A                     N/A            2000-01-01  N/A                        N/A                         N/A                          N/A')
            }

    }
    
    def "equalsDatetime"() {
        when:
        def result = generate("#date(dateTimeValue)==#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7776].contains('23311  N/A           N/A                  N/A                     N/A            N/A         2000-01-01T00:00:00+00:00  N/A                         N/A                          N/A')
            }

    }
    
    
    def "equalsOrIntString"() {
        when:
            def result = generate("id==0 || stringValue=='value2'")
        then:
            verifyAll(result.stdout) {
                size()==7778
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7777].contains('15551  value2        N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "equalsOrLongDouble"() {
        when:
            def result = generate("longValue==9223372036854775807L || doubleValue==0.7")
        then:
            verifyAll(result.stdout) {
                size()==7129
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1081].contains('5400   value1        9223372036854775807  -0.7                    true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7128].contains('22247  N/A           N/A                  0.7                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "equalsOrBoolDate"() {
        when:
            def result = generate("booleanValue==false || dateValue=='2000-01-01'")
        then:
            verifyAll(result.stdout) {
                size()==12961
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[49].contains('96     value1        1000                 0.7                     false          2030-12-31  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[12960].contains('23279  N/A           N/A                  N/A                     N/A            2000-01-01  N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "equalsOrLongDateTime"() {
        when:
            def result = generate("longValue==9223372036854775807L || #date(dateTimeValue)==#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==10369
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1737].contains('5192   value1        9223372036854775807  0.7                     true           2000-01-01  2030-12-31T23:59:59+02:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[10368].contains('23311  N/A           N/A                  N/A                     N/A            N/A         2000-01-01T00:00:00+00:00  N/A                         N/A                          N/A ')
            }
    }
    
    def "smallerAndGreaterThanInt"() {
        when:
            def result = generate("id<9999 && id>1111")
        then:
            verifyAll(result.stdout) {
                size()==8888
                it[0].contains('Id    String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('1112  value1        1000                 N/A                     true           2030-12-31  2030-12-31T23:59:59+02:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[8887].contains('9998  value2        -1000                1.7976931348623157E308  true           N/A         2030-12-31T23:59:59+02:00  N/A                         N/A')
            }
    }
    
    def "smallerAndGreaterThanLong"() {
        when:
            def result = generate("longValue<9223372036854775807L && longValue>1111L")
        then:
            verifyAll(result.stdout) {
                size()==3889
                it[0].contains('Id     String value  Long value  Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('2592   value1        2000        0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[3888].contains('19439  N/A           2000        N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "smallerAndGreaterThanDouble"() {
        when:
            def result = generate("doubleValue<1.7976931348623157E308 && doubleValue>-0.7")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id     String value  Long value           Double value  Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7           true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7776].contains('22679  N/A           N/A                  1.4           N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "smallerAndGreaterThanDateTime"() {
        when:
            def result = generate("#date(dateTimeValue)<#date('2031-12-31T23:59:59+02:00') && #date(dateTimeValue)>#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('8      value1        1000                 0.7                     true           2000-01-01  2030-12-31T23:59:59+02:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[7776].contains('23319  N/A           N/A                  N/A                     N/A            N/A         2030-12-31T23:59:59+02:00  N/A                         N/A                          N/A')
            }
    }
    
    def "smallerAndGreaterEqualInt"() {
        when:
            def result = generate("id<=9999 && id>=1111")
        then:
            verifyAll(result.stdout) {
                size()==8890
                it[0].contains('Id    String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object  Nested object array  Nested string aray')
                it[1].contains('1111  value1        1000                 N/A                     true           2030-12-31  2000-01-01T00:00:00+00:00  N/A            N/A                  N/A')
                it[8889].contains('9999  value2        -1000                1.7976931348623157E308  true           N/A         2030-12-31T23:59:59+02:00  N/A            N/A                  N/A')
            }
    }
    
    def "smallerAndGreaterEqualLong"() {
        when:
            def result = generate("longValue<=9223372036854775807L && longValue>=1000L")
        then:
            verifyAll(result.stdout) {
                size()==11665
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[11664].contains('22031  N/A           9223372036854775807  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "smallerAndGreaterEqualDouble"() {
        when:
            def result = generate("doubleValue<=1.7976931348623157E308 && doubleValue>=-0.7")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23111  N/A           N/A                  1.7976931348623157E308  N/A            N/A         N/A                        N/A                         N/A                          N/A')
            }
    }
    
    def "smallerAndGreaterEqualDateTime"() {
        when:
            def result = generate("#date(dateTimeValue)<=#date('2031-12-31T23:59:59+02:00') && #date(dateTimeValue)>=#date('2000-01-01T00:00:00+00:00')")
        then:
            verifyAll(result.stdout) {
                size()==15553
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('23319  N/A           N/A                  N/A                     N/A            N/A         2030-12-31T23:59:59+02:00  N/A                         N/A                          N/A')
            }
    }
    
    
    def "smallerAndGreaterEqualDateTimeNow"() {
        when:
            def result = generate("(#date(dateTimeValue)<=#now('+14610d') && #date(dateTimeValue)>=#now('+1000d'))")
        then:
            verifyAll(result.stdout) {
                size()==7777
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('8      value1        1000                 0.7                     true           2000-01-01  2030-12-31T23:59:59+02:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4 ')
                it[7776].contains('23319  N/A           N/A                  N/A                     N/A            N/A         2030-12-31T23:59:59+02:00  N/A                         N/A                          N/A')
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
                it[0].contains('Id     String value  Long value           Double value            Boolean value  Date value  Date time value            Nested object string value  Nested object boolean value  Nested string aray')
                it[1].contains('0      value1        1000                 0.7                     true           2000-01-01  2000-01-01T00:00:00+00:00  nestedObjectValue1          true                         nestedArrayValue3, nestedArrayValue4')
                it[15552].contains('15551  value2        N/A                  N/A                     N/A            N/A         N/A                        N/A                         N/A                          N/A')
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
