package org.eipgrid.jql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.Entity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1) select 가 지정되지 않으면, 쿼리 문에 사용된 Entity 들의 Property 들을 검색 결과에 포함시킨다.
 * 2) select 를 이용하여 검색 결과에 포함할 Property 들을 명시할 수 있다.
 *    - Joined Property 명만 명시된 경우, 해당 Joined Entity 의 모든 Property 를 선택.
 */
public interface JqlRestApi {

    @Getter
    class Response {

        @Schema(implementation = Object.class)
        private Map<String, Object> metadata;
        private Object content;

        @JsonIgnore
        private JqlSelect.ResultMap resultMapping;

        @Getter
        @JsonIgnore
        private JqlQuery query;

        private Response(Object content, JqlSelect.ResultMap resultMapping) {
            this.content = content;
            this.resultMapping = resultMapping;
        }

        public static Response of(Object content, JqlSelect select) {
            if (isJpaType(content)) {
                return new JpaFilter(content, select.getPropertyMap());
            } else {
                return new Response(content, select.getPropertyMap());
            }
        }

        private static boolean isJpaType(Object content) {
            if (content == null) return false;
            Class clazz = content.getClass();
            if (List.class.isAssignableFrom(clazz)) {
                List list = (List)content;
                if (list.size() == 0) return false;
                clazz = list.get(0).getClass();
            }
            return clazz.getAnnotation(Entity.class) != null;
        }

        public void setProperty(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
        }

        @JsonSerialize(using = JpaFilter.Serializer.class)
        public static class JpaFilter extends Response {
            public static final String JQL_RESULT_MAPPING_KEY = "jql-result-mapping";

            /*internal*/ JpaFilter(Object content, JqlSelect.ResultMap resultMappings) {
                super(content, resultMappings);
            }

            static class Serializer extends StdSerializer<JpaFilter> {
                protected Serializer() {
                    super(JqlRestApi.Response.JpaFilter.class);
                }

                @Override
                public void serialize(JqlRestApi.Response.JpaFilter value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                    provider.setAttribute(JQL_RESULT_MAPPING_KEY, value.getResultMapping());
                    JsonSerializer<Object> s = provider.findValueSerializer(JqlRestApi.Response.class);
                    s.serialize(value, gen, provider);
                }
            }
        }
    }


    default Response search(JqlEntitySet entitySet, String select, String[] orders, Integer page, Integer limit, Map<String, Object> filter) {
        JqlQuery query = entitySet.createQuery(filter);
        query.select(select);
        if (orders != null) query.sort(orders);
        boolean needPagination = false;
        if (limit != null) {
            query.limit(limit);
            if (page != null) {
                query.offset(page * limit);
            }
            needPagination = limit > 0 && query.getOffset() > 0;
        }
        List<Object> result = query.getResultList();
        Response resp = Response.of(result, query.getSelection());
        resp.query = query;
        if (needPagination) {
            resp.setProperty("totalElements", query.count());
        }
        return resp;
    }

    static Sort.Order parseOrder(String column) {
        char first_ch = column.charAt(0);
        boolean ascend = first_ch != '-';
        String name = (ascend && first_ch != '+') ? column : column.substring(1);
        return ascend ? Sort.Order.asc(name) : Sort.Order.desc(name);
    }

    static Sort buildSort(String[] orders) {
        if (orders == null || orders.length == 0) {
            return Sort.unsorted();
        }
        ArrayList<Sort.Order> _orders = new ArrayList<>();
        for (String column : orders) {
            Sort.Order order = parseOrder(column);
            _orders.add(order);
        }
        return Sort.by(_orders);
    }

    default JqlEntitySet.InsertPolicy parseInsertPolicy(String onConflict) {
        JqlEntitySet.InsertPolicy insertPolicy;
        if (onConflict == null) {
            insertPolicy = JqlEntitySet.InsertPolicy.ErrorOnConflict;
        } else {
            switch (onConflict.toLowerCase()) {
                case "error":
                    insertPolicy = JqlEntitySet.InsertPolicy.ErrorOnConflict;
                    break;
                case "ignore":
                    insertPolicy = JqlEntitySet.InsertPolicy.IgnoreOnConflict;
                    break;
                case "update":
                    insertPolicy = JqlEntitySet.InsertPolicy.UpdateOnConflict;
                    break;
                default:
                    throw new IllegalArgumentException("unknown onConflict option: " + onConflict);
            }
        }
        return insertPolicy;
    }
}
