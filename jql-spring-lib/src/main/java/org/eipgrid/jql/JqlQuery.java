package org.eipgrid.jql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public abstract class JqlQuery<ENTITY> {
    @Getter
    @JsonIgnore
    @Schema(implementation = String.class)
    private JqlSelect selection;

    @Getter
    @JsonIgnore
    @Schema(implementation = String.class)
    private Sort sort;

    @Getter
    private int offset;

    @Getter
    private int limit;

    protected void select(JqlSelect selection) {
        this.selection = selection;
    }

    @JsonProperty()
    public final JqlQuery<ENTITY> select(String jqlSelectStatement) {
        select(JqlSelect.of(jqlSelectStatement));
        return this;
    }

    public final JqlQuery<ENTITY> select(String[] selectedPropertyNames) {
        select(JqlSelect.of(selectedPropertyNames));
        return this;
    }

    public JqlQuery<ENTITY> sort(Sort sort) {
        this.sort = sort;
        return this;
    }

    public final JqlQuery<ENTITY> sort(String orders[]) {
        sort(JqlRestApi.buildSort(orders));
        return this;
    }

    public JqlQuery<ENTITY> offset(int offset) {
        this.offset = offset;
        return this;
    }

    public JqlQuery<ENTITY> limit(int limit) {
        this.limit = limit;
        return this;
    }

    public abstract List<ENTITY> getResultList(OutputFormat outputType);

    public final List<ENTITY> getResultList() { return getResultList(OutputFormat.Object); }

    public abstract long count();

    public final ENTITY getSingleResult() {
        List<ENTITY> res = getResultList();
        return res.size() > 0 ? res.get(0) : null;
    }

    public String getExecutedQuery() { return null; }
}
