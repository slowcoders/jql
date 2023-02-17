package org.eipgrid.jql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import org.eipgrid.jql.parser.JqlFilter;
import org.eipgrid.jql.schema.QResultMapping;
import org.springframework.data.domain.Sort;

import java.util.*;

/**
 * 1) select 가 지정되지 않으면, 쿼리 문에 사용된 Entity 들의 Property 들을 검색 결과에 포함시킨다.
 * 2) select 를 이용하여 검색 결과에 포함할 Property 들을 명시할 수 있다.
 *    - Joined Property 명만 명시된 경우, 해당 Joined Entity 의 모든 Property 를 선택.
 */
@Getter
public class JqlQuery {

    private final JqlEntitySet repository;
    private final JqlSelect select;
    private final JqlFilter filter;


    private Sort sort;
    private int offset;
    private int limit;

    /*package*/ String executedQuery;
    /*package*/ Object extraInfo;

    protected JqlQuery(JqlEntitySet repository, JqlSelect select, JqlFilter filter) {
        this.repository = repository;
        if (select == null) {
            select = JqlSelect.Auto;
        }
        this.select = select;
        this.filter = filter;
    }

    private JqlQuery(JqlEntitySet repository, JqlSelect select, Map<String, Object> filter) {
        this(repository, select, repository.createFilter(filter));
    }

    public static JqlQuery of(JqlEntitySet repository, JqlSelect select, Sort sort, int offset, int limit, Map<String, Object> filter) {
        JqlQuery query = new JqlQuery(repository, select, filter);
        query.sort = sort;
        query.offset = offset;
        query.limit = limit;
        return query;
    }

    public static JqlQuery of(JqlEntitySet repository, JqlSelect select, Map<String, Object> filter) {
        return new JqlQuery(repository, select, filter);
    }
    
    public boolean needPagination() {
        return offset >= 0 && limit > 0;
    }

    public Response execute() {
        List<?> result = repository.find(this, OutputFormat.Object);
        Response resp = new Response(result, filter);
        if (needPagination()) {
            resp.setProperty("totalElements", this.count());
        }
        return resp;
    }

    public long count() {
        return repository.count(filter);
    }

    @Data
    public static class Request {
        private String select;
        private String sort;
        private Integer page;
        private Integer limit;

        @Schema(implementation = Object.class)
        private HashMap filter;

        public JqlQuery buildQuery(JqlEntitySet table) {
            JqlSelect _select = JqlSelect.of(select);
            Sort _sort = parseSort(sort);
            int _limit = limit == null ? 0 : limit;
            int _page = page == null ? -1 : page;

            JqlQuery query = JqlQuery.of(table, _select, _sort, _page * _limit, _limit, filter);
            return query;
        }
    }

    @Getter
    public static class Response {

        @Schema(implementation = Object.class)
        private Map<String, Object> metadata;
        private Object content;

        @JsonIgnore
        private QResultMapping resultMapping;

        private Response(Object content, QResultMapping resultMapping) {
            this.content = content;
            this.resultMapping = resultMapping;
        }

        public void setProperty(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
        }
    }

    public static Sort.Order createOrder(String column) {
        char first_ch = column.charAt(0);
        boolean ascend = first_ch != '-';
        String name = (ascend && first_ch != '+') ? column : column.substring(1);
        return ascend ? Sort.Order.asc(name) : Sort.Order.desc(name);
    }

    public static Sort parseSort(String orders) {
        String[] properties = null;
        if (orders != null) {
            orders = orders.trim();
            if (orders.length() > 0) {
                properties = orders.split("\\s*,\\s*");
            }
        }
        return buildSort(properties);
    }

    public static Sort buildSort(String[] orders) {
        if (orders == null || orders.length == 0) {
            return Sort.unsorted();
        }
        ArrayList<Sort.Order> _orders = new ArrayList<>();
        for (String column : orders) {
            Sort.Order order = createOrder(column);
            _orders.add(order);
        }
        return Sort.by(_orders);
    }

}
