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
package com.fortify.cli.common.output.writer.record.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.expression.Expression;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.impl.RecordWriterExpr.ExpressionWriter;
import com.fortify.cli.common.spel.validator.AbstractSimpleSpelNodeValidator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecordWriterExpr extends AbstractRecordWriter<ExpressionWriter> {
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    @Getter private final RecordWriterConfig config;
    
    @Override
    protected void append(ExpressionWriter out, ObjectNode formattedRecord) throws IOException {
        out.append(formattedRecord);
    }
    
    @Override
    protected Function<ObjectNode, ObjectNode> createRecordFormatter(ObjectNode objectNode) throws IOException {
        return Function.identity();
    }   
    
    @Override
    protected void close(ExpressionWriter out) throws IOException {
        out.close();
    }
    
    @Override
    protected void closeWithNoData(Writer writer) throws IOException {
        writer.close();
    }
    
    @Override
    protected ExpressionWriter createOut(Writer writer, ObjectNode formattedRecord) throws IOException {
        if ( formattedRecord==null ) { return null; }
        return new ExpressionWriter(writer, getValidatedExpression());
    }
    
    private Expression getValidatedExpression() {
        var result = getExpression();
        new OutputExpressionValidator(result).visit();
        return result;
    }
    
    private Expression getExpression() {
        var expressionString = config.getArgs();
        try {
            return PARSER.parseExpression(
                    insertControlCharacters(expressionString), 
                    new TemplateParserContext("{", "}"));
        } catch ( Exception e ) {
            throw new FcliSimpleException(String.format("Output expression template cannot be parsed; please check expression syntax\n\tMessage: %s\n\tTemplate expression: %s", e.getMessage(), expressionString));
        }
    }
    
    private static final String insertControlCharacters(String s) {
        return s.replaceAll("\\\\t", "\t")
                .replaceAll("\\\\b", "\b")
                .replaceAll("\\\\n", "\n")
                .replaceAll("\\\\r", "\r")
                .replaceAll("\\\\f", "\f");
    }
    
    @RequiredArgsConstructor
    protected final class ExpressionWriter implements Closeable { 
        private final Writer writer;
        private final Expression expression;
        
        public void append(ObjectNode formattedRecord) throws IOException {
            writer.write(eval(formattedRecord));
        }
        
        @Override
        public void close() throws IOException {
            writer.flush();
            writer.close();
        }
        
        private String eval(ObjectNode record) {
            try {
                var result = JsonHelper.evaluateSpelExpression(record, expression, String.class);
                return result==null ? "" : result;
            } catch ( Exception e ) {
                throw new FcliSimpleException(String.format("Error evaluating output expression:\n\tMessage: %s\n\tExpression: %s\n\tRecord: %s", e.getMessage(), config.getArgs(), record.toPrettyString().replace("\n", "\n\t\t")));
            }
        }
    }
    
    private final class OutputExpressionValidator extends AbstractSimpleSpelNodeValidator {
        public OutputExpressionValidator(Expression e) { super(e); }
        
        @Override
        protected void visit(Expression expression) {
            if ( expression instanceof CompositeStringExpression ) {
                var compositeExpression = (CompositeStringExpression)expression;
                Stream.of(compositeExpression.getExpressions()).forEach(this::visit);
            } else {
                super.visit(expression);
            }
        }
        
        @Override
        protected String formatValidationError(SpelNode node, String msg) {
            return String.format("Invalid output expression:\n\tMessage: %s\n\tExpression: %s\n\tNode: %s", msg, config.getArgs(), node.toStringAST());
        }
        @Override
        protected RuntimeException getValidationException(String msg) {
            return new IllegalStateException(msg);
        }            
    }
}
