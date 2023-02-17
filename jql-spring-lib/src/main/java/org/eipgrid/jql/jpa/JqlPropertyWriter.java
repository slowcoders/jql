package org.eipgrid.jql.jpa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QResultMapping;

import java.util.Stack;

public class JqlPropertyWriter extends BeanPropertyWriter {
//    private static final String JQL_STACK_KEY = "jql-entity-stack";
    private static final String JQL_RESULT_MAPPING_KEY = "jql-result-mapping";
    private final BeanPropertyWriter writer;

    public JqlPropertyWriter(BeanPropertyWriter writer) {
        super(writer);
        this.writer = writer;
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen,
                                   SerializerProvider prov) throws Exception {
        super.serializeAsElement(bean, gen, prov);
    }


//    private Stack<Object> getStack(SerializerProvider provider) {
//        Stack<Object> stack = (Stack<Object>) provider.getAttribute(JQL_STACK_KEY);
//        if (stack == null) {
//            stack = new Stack<>();
//            provider.setAttribute(JQL_STACK_KEY, stack);
//        }
//        return stack;
//    }

    @Override
    public void serializeAsField(Object bean,
                                 JsonGenerator gen,
                                 SerializerProvider prov) throws Exception {
        JavaType valueType = writer.getType();
        if (valueType.getContentType() != null) {
            valueType = valueType.getContentType();
        }

        QResultMapping mapping = (QResultMapping) prov.getAttribute(JQL_RESULT_MAPPING_KEY);
        if (mapping == null) {
            if (bean instanceof JqlQuery.Response) {
                prov.setAttribute(JQL_RESULT_MAPPING_KEY, ((JqlQuery.Response)bean).getResultMapping());
            }
            mapping = (QResultMapping) prov.getAttribute(JQL_RESULT_MAPPING_KEY);
            prov.setAttribute(JQL_RESULT_MAPPING_KEY, mapping);
        } else {
            String pname = this.getName();
            QColumn column = mapping.getSchema().findColumn(pname);
            boolean isRef = (column != null && column.isJsonNode()) ||
                mapping.getSchema().getEntityJoinBy(pname) != null;
            if (isRef) {
                QResultMapping child = mapping.getChildMapping(this.getName());
                if (child != null) {
                    prov.setAttribute(JQL_RESULT_MAPPING_KEY, child);
                    super.serializeAsField(bean, gen, prov);
                    prov.setAttribute(JQL_RESULT_MAPPING_KEY, mapping);
                }
                return;
            }
        }
        Object value = gen.getCurrentValue();
//        Stack stack = getStack(prov);
//        if (stack.contains(value)) {
//            return;
//        }
//        stack.push(value);
        super.serializeAsField(bean, gen, prov);
//        stack.pop();
    }


}