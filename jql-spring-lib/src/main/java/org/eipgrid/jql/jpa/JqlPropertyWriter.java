package org.eipgrid.jql.jpa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import org.eipgrid.jql.JqlRestApi;
import org.eipgrid.jql.js.JsType;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QResultMapping;

import javax.persistence.Entity;
import java.util.Map;

public class JqlPropertyWriter extends BeanPropertyWriter {
//    private static final String JQL_STACK_KEY = "jql-entity-stack";
    private static final String JQL_RESULT_MAPPING_KEY = "jql-result-mapping";

    public static final Class hibernateProxyClass;

    static {
        Class c;
        try {
            c = Class.forName("org.hibernate.proxy.HibernateProxy");
        } catch (Exception e) {
            c = null;
        }
        hibernateProxyClass = c;
    }

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
        boolean check_ref = valueType.getContentType() != null;
        if (!check_ref) {
            Class<?> clazz = valueType.getRawClass();
            check_ref = // !JsType.of(clazz).isPrimitive();
                clazz.getAnnotation(Entity.class) != null ||
                        Map.class.isAssignableFrom(clazz) ||
                        JsonNode.class.isAssignableFrom(clazz);
        }

        Map<String, Object> mapping = (Map<String, Object>) prov.getAttribute(JQL_RESULT_MAPPING_KEY);
        if (mapping == null) {
            if (bean instanceof JqlRestApi.Response) {
                prov.setAttribute(JQL_RESULT_MAPPING_KEY, ((JqlRestApi.Response)bean).getResultMapping());
            }
            mapping = (Map<String, Object>) prov.getAttribute(JQL_RESULT_MAPPING_KEY);
            prov.setAttribute(JQL_RESULT_MAPPING_KEY, mapping);
        } else if (check_ref) {
            String pname = this.getName();
            Object column = mapping.get(pname);
            if (column != null) {
                prov.setAttribute(JQL_RESULT_MAPPING_KEY, column);
                super.serializeAsField(bean, gen, prov);
                prov.setAttribute(JQL_RESULT_MAPPING_KEY, mapping);
            }
            return;
        }
//        Object value = gen.getCurrentValue();
//        Stack stack = getStack(prov);
//        if (stack.contains(value)) {
//            return;
//        }
//        stack.push(value);
        super.serializeAsField(bean, gen, prov);
//        stack.pop();
    }


}