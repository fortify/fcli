package com.fortify.cli.ftest.core;

import com.fortify.cli.ftest._common.Fcli
import com.fortify.cli.ftest._common.Fcli.FcliResult
import com.fortify.cli.ftest._common.spec.FcliBaseSpec
import com.fortify.cli.ftest._common.spec.Prefix

@Prefix("core.query")
class QuerySpec extends FcliBaseSpec {
    private static final FcliResult generate(String query) {
        def args = "util sample-data list"
        if ( query!=null ) { args+=" -q "+query.replace(" ", "\\ ") }
        return Fcli.run(args)
    }
    
    def "contains"() {
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
    
}