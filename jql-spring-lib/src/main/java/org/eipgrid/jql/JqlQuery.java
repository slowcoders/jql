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
public abstract class JqlQuery {

    protected static int SingleEntityOffset = -100010001;
    private final JqlSelect select;
    private final JqlFilter filter;


    private Sort sort;
    private int offset;
    private int limit;

    /*package*/ String executedQuery;
    /*package*/ Object extraInfo;

    protected JqlQuery(JqlSelect select, JqlFilter filter) {
        if (select == null) {
            select = JqlSelect.Auto;
        }
        this.select = select;
        this.filter = filter;
    }


    public JqlQuery(JqlSelect select, Sort sort, int offset, int limit, JqlFilter filter) {
        this(select, filter);
        this.sort = sort;
        this.offset = offset;
        this.limit = limit;
    }

    public boolean needPagination() {
        return offset >= 0 && limit > 0;
    }

    protected abstract List<?> executeQuery(OutputFormat outputType);

    public Response execute() {
        return execute(OutputFormat.Object);
    }

    public Response execute(OutputFormat outputType) {
        List<?> result = executeQuery(OutputFormat.Object);
        Object content = null;
        if (limit != 1 || offset != SingleEntityOffset) {
            content = result;
        }
        else if (result.size() > 0) {
            content = result.get(0);
        }
        Response resp = new Response(this, content, filter);
        if (needPagination()) {
            resp.setProperty("totalElements", this.count());
        }
        return resp;
    }

    public abstract long count();

    @Data
    public static class Request {
        private String select;
        private String sort;
        private Integer page;
        private Integer limit;

        @Schema(implementation = Object.class)
        private HashMap filter;

        public Response execute(JqlEntitySet table) {
            JqlSelect _select = JqlSelect.of(select);
            Sort _sort = parseSort(sort);
            int _limit = limit == null ? 0 : limit;
            int _page = page == null ? -1 : page;

            JqlQuery query = table.createQuery(filter, _select);
            query.sort = _sort;
            query.limit = _limit;
            query.offset = _page * _limit;
            return query.execute();
        }
    }


    @Getter
    public static class Response {

        @Schema(implementation = Object.class)
        private Map<String, Object> metadata;
        private Object content;

        @JsonIgnore
        private QResultMapping resultMapping;

        @JsonIgnore
        private JqlQuery query;

        private Response(JqlQuery query, Object content, QResultMapping resultMapping) {
            this.query = query;
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
