package org.eipgrid.jql.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.eipgrid.jql.jpa.JqlJacksonModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class DefaultJqlConfig {


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
        JqlJacksonModule hm = new JqlJacksonModule();
        return hm;
    }

}

