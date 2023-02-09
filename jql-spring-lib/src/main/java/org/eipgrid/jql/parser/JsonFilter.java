package org.eipgrid.jql.parser;

class JsonFilter extends EntityFilter {
    private final String key;

    JsonFilter(EntityFilter parentQuery, String key) {
        super(parentQuery);
        this.key = key;
    }

    @Override
    public String getMappingAlias() {
        return this.key;
    }

    @Override
    public EntityFilter makeSubNode(String key, JqlParser.NodeType nodeType) {
        if (nodeType == JqlParser.NodeType.Leaf) {
            return this;
        }
        EntityFilter entity = subFilters.get(key);
        if (entity == null) {
            entity = new JsonFilter(this, key);
            subFilters.put(key, entity);
        }
        return entity;
    }


    @Override
    public String getColumnName(String key) {
        return key.substring(key.lastIndexOf('.') + 1);
    }


}
