/**
 * Copyright 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 */
package com.fortify.cli.common.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;

import lombok.RequiredArgsConstructor;

public class InsertFileContentsPOJONode extends POJONode {
    private static final long serialVersionUID = 1L;

    public InsertFileContentsPOJONode(Path filePath, String indent) {
        super(new InsertFileContents(filePath, indent));
        if ( !Files.isReadable(filePath) ) {
            throw new FcliSimpleException("File to be inserted is not readable: "+filePath.toString());
        }
    }

    public InsertFileContentsPOJONode(String filePath, String indent) {
        this(Path.of(filePath), indent);
    }

    @RequiredArgsConstructor @Reflectable
    @JsonSerialize(using=InsertFileContentsSerializer.class)
    public static final class InsertFileContents {
        private final Path filePath;
        private final String indent;
    }
    
    @Reflectable
    public static final class InsertFileContentsSerializer extends StdSerializer<InsertFileContents> {
        private static final long serialVersionUID = 1L;

        public InsertFileContentsSerializer() {
            super(InsertFileContents.class);
        }

        @Override
        public void serialize(InsertFileContents value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            try (var reader = Files.newBufferedReader(value.filePath) ) {
                gen.writeRawValue(""); // Insert field separator
                var buffer = new char[1024];
                var charsRead=0;
                while (charsRead!=-1) {
                    charsRead = reader.read(buffer);
                    if ( charsRead!=-1 ) {
                        var indentedString = new String(buffer, 0, charsRead).replaceAll("\n", "\n"+value.indent);
                        gen.writeRaw(indentedString);
                    }
                }
            }
        }
    }
}
