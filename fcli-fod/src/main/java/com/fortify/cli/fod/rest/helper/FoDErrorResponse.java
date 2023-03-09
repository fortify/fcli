package com.fortify.cli.fod.rest.helper;

import java.util.ArrayList;

import com.fortify.cli.common.json.JsonNodeHolder;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;

@ReflectiveAccess
@Data
@EqualsAndHashCode(callSuper=false)
public class FoDErrorResponse extends JsonNodeHolder {
    ArrayList<FoDError> errors;
    @Data
    public class FoDError {
        int errorCode;
        String message;
    }

    @Override
    public String toString() {
        String result = "\n";
        for (FoDError error : errors) {
            int errorNumber = errors.indexOf(error) + 1;
            result += errorNumber + ") " + error.getMessage() + (errorNumber > 1 ? "\n" : "");
        }
        return result;
    }
}


