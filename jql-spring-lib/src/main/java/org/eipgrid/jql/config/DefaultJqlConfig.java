package org.eipgrid.jql.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import lombok.SneakyThrows;
import org.eipgrid.jql.JqlRestApi;
import org.eipgrid.jql.jpa.JqlPropertyWriter;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class DefaultJqlConfig {

    /**
     * Serialize 시 무한 루프 방지를 위한 Jackson 모듈
     * 참고) @JsonBackReference 와 @JsonIdentityInfo 대체
     */
    static class JPAJacksonModule extends com.fasterxml.jackson.databind.Module { // Hibernate5Module {
        @Override
        public String getModuleName() {
            return "JQLJackson";
        }

        @Override
        public Version version() {
            return Version.unknownVersion();
        }

        @Override
        public void setupModule(SetupContext context) {
//            super.setupModule(context);
            context.addBeanSerializerModifier(new BeanSerializerModifier() {
//                @Override
//                public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
//                    Class<?> clazz = beanDesc.getBeanClass();
//                    if (clazz.getAnnotation(Entity.class) != null) {
//                        serializer = new JPAEntitySerializer(beanDesc, serializer);
//                    } else if (JqlQuery.Response.class.isAssignableFrom(clazz)) {
//                        serializer = new JqlResponseSerializer(beanDesc, serializer);
//                    }
//                    return serializer;
//                }

                @Override
                public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                                 BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
                    for (int i = 0; i < beanProperties.size(); i++) {
                        BeanPropertyWriter writer = beanProperties.get(i);
                        Class<?> clazz = beanDesc.getBeanClass();
                        if (clazz.getAnnotation(Entity.class) != null
                            || JqlRestApi.Response.class.isAssignableFrom(clazz)) {
                            beanProperties.set(i, new JqlPropertyWriter(writer));
                        }
                    }
                    return beanProperties;
                }
            });
        }
    }



    @Component
    @ConfigurationPropertiesBinding
    public static class StringToJsonNodeDeserializer implements Converter<String, ObjectNode> {

        private ObjectMapper om;

        public StringToJsonNodeDeserializer() {
            om = new ObjectMapper();
        }

        @SneakyThrows
        @Override
        public ObjectNode convert(String source) {
            if (source == null) return null;
            return (ObjectNode)om.readTree(source);
        }
    }

    @Component
    @ConfigurationPropertiesBinding
    public static class StringToHashMapDeserializer implements Converter<String, HashMap> {

        private ObjectMapper om;

        public StringToHashMapDeserializer() {
            om = new ObjectMapper();
        }

        @SneakyThrows
        @Override
        public HashMap convert(String source) {
            if (source == null) return null;
            return om.readValue(source, HashMap.class);
        }
    }


    @Component
    @ConfigurationPropertiesBinding
    public static class MultiPartFileToStringDeserializer implements Converter<MultipartFile, String> {

        public MultiPartFileToStringDeserializer() {
        }

        @SneakyThrows
        @Override
        public String convert(MultipartFile source) {
            if (source == null) return null;
            byte[] bytes = source.getBytes();
            String s = new String(bytes, StandardCharsets.UTF_8);
            return s;
        }
    }

    @Bean
    public Module jqlJacksonModule() {
        JPAJacksonModule hm = new JPAJacksonModule();
        return hm;
    }

}

