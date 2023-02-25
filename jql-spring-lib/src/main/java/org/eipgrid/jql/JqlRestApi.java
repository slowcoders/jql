package org.eipgrid.jql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Sort;

import javax.persistence.Entity;
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
        private Map<String, Object> resultMapping;

        @Getter
        @JsonIgnore
        private JqlQuery query;

        private Response(Object content, Map<String, Object> resultMapping) {
            this.content = content;
            this.resultMapping = resultMapping;
        }

        public static Response of(Object content, JqlSelect select) {
            if (isJpaType(content)) {
                return new JpaFilter(content, select.getResultMappings());
            } else {
                return new Response(content, select.getResultMappings());
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

        private static class JpaFilter extends Response {
            public JpaFilter(Object content, Map<String, Object> resultMappings) {
                super(content, resultMappings);
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

}
