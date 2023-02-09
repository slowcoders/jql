package org.eipgrid.jql.jpa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.eipgrid.jql.JqlQuery;

import java.io.IOException;
import java.util.Stack;

public class JqlResponseSerializer extends StdSerializer<JqlQuery.Response> {
    private final JsonSerializer<JqlQuery.Response> serializer;
    private static final String JQL_STACK_KEY = "jql-entity-stack";

    public JqlResponseSerializer(BeanDescription beanDesc, JsonSerializer<?> serializer) {
        super((Class<JqlQuery.Response>)beanDesc.getBeanClass());
        this.serializer = (JsonSerializer<JqlQuery.Response>) serializer;
    }

    @Override
    public void serialize(
            JqlQuery.Response value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        Stack<Object> stack = new Stack<>();
        provider.setAttribute(JQL_STACK_KEY, stack);
        serializer.serialize(value, gen, provider);
    }
}