package org.eipgrid.jql.jpa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import org.eipgrid.jql.JqlLeafProperty;
import org.eipgrid.jql.JqlRestApi;
import org.eipgrid.jql.js.JsType;

import javax.persistence.Id;
import java.util.Map;

public class JpaPropertyFilter extends BeanPropertyWriter {
    private static final String JQL_RESULT_MAPPING_KEY = JqlRestApi.Response.JpaFilter.JQL_RESULT_MAPPING_KEY;

    private final BeanPropertyWriter writer;
    private final boolean isId;
    private final boolean isLeaf;

    public JpaPropertyFilter(BeanPropertyWriter writer) {
        super(writer);
        this.writer = writer;
        this.isId = writer.getAnnotation(Id.class) != null;
        this.isLeaf = writer.getAnnotation(JqlLeafProperty.class) != null || 
                JsType.of(writer.getType().getRawClass()).isPrimitive();
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen,
                                   SerializerProvider prov) throws Exception {
        super.serializeAsElement(bean, gen, prov);
    }


    @Override
    public void serializeAsField(Object bean,
                                 JsonGenerator gen,
                                 SerializerProvider prov) throws Exception {

        Map<String, Object> mapping = (Map<String, Object>) prov.getAttribute(JQL_RESULT_MAPPING_KEY);
        if (!this.isId && mapping != null) {
            String pname = this.getName();
            Object column = mapping.get(pname);
            if (column != null) {
                if (!isLeaf) {
                    prov.setAttribute(JQL_RESULT_MAPPING_KEY, column);
                    writer.serializeAsField(bean, gen, prov);
                    prov.setAttribute(JQL_RESULT_MAPPING_KEY, mapping);
                    return;
                }
            }
            else if (!isLeaf || (!mapping.isEmpty() && mapping.get("*") == null)) {
                return;
            }
        }
        writer.serializeAsField(bean, gen, prov);
    }
}