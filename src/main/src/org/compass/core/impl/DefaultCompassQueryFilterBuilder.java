package org.compass.core.impl;

import org.compass.core.CompassQuery;
import org.compass.core.CompassQueryFilter;
import org.compass.core.CompassQueryFilterBuilder;
import org.compass.core.engine.SearchEngineQueryFilterBuilder;
import org.compass.core.mapping.ResourcePropertyLookup;
import org.compass.core.spi.InternalCompassSession;

/**
 * @author kimchy
 */
public class DefaultCompassQueryFilterBuilder implements CompassQueryFilterBuilder {

    public static class DefaultCompassBooleanQueryFilterBuilder implements CompassBooleanQueryFilterBuilder {

        private SearchEngineQueryFilterBuilder.SearchEngineBooleanQueryFilterBuilder filterBuilder;

        public DefaultCompassBooleanQueryFilterBuilder(SearchEngineQueryFilterBuilder.SearchEngineBooleanQueryFilterBuilder filterBuilder) {
            this.filterBuilder = filterBuilder;
        }

        public CompassBooleanQueryFilterBuilder and(CompassQueryFilter filter) {
            filterBuilder.and(((DefaultCompassQueryFilter) filter).getFilter());
            return this;
        }

        public CompassBooleanQueryFilterBuilder or(CompassQueryFilter filter) {
            filterBuilder.or(((DefaultCompassQueryFilter) filter).getFilter());
            return this;
        }

        public CompassBooleanQueryFilterBuilder andNot(CompassQueryFilter filter) {
            filterBuilder.andNot(((DefaultCompassQueryFilter) filter).getFilter());
            return this;
        }

        public CompassBooleanQueryFilterBuilder xor(CompassQueryFilter filter) {
            filterBuilder.xor(((DefaultCompassQueryFilter) filter).getFilter());
            return this;
        }

        public CompassQueryFilter toFilter() {
            return new DefaultCompassQueryFilter(filterBuilder.toFilter());
        }
    }

    private InternalCompassSession session;

    private SearchEngineQueryFilterBuilder filterBuilder;

    public DefaultCompassQueryFilterBuilder(SearchEngineQueryFilterBuilder filterBuilder, InternalCompassSession session) {
        this.filterBuilder = filterBuilder;
        this.session = session;
    }

    public CompassQueryFilter between(String name, Object low, Object high, boolean includeLow, boolean includeHigh) {
        ResourcePropertyLookup lookup = session.getMapping().getResourcePropertyLookup(name);
        return new DefaultCompassQueryFilter(
                filterBuilder.between(lookup.getPath(), lookup.getValue(low), lookup.getValue(high), includeLow, includeHigh));
    }

    public CompassQueryFilter lt(String name, Object value) {
        ResourcePropertyLookup lookup = session.getMapping().getResourcePropertyLookup(name);
        return new DefaultCompassQueryFilter(
                filterBuilder.lt(lookup.getPath(), lookup.getValue(value)));
    }

    public CompassQueryFilter le(String name, Object value) {
        ResourcePropertyLookup lookup = session.getMapping().getResourcePropertyLookup(name);
        return new DefaultCompassQueryFilter(
                filterBuilder.le(lookup.getPath(), lookup.getValue(value)));
    }

    public CompassQueryFilter gt(String name, Object value) {
        ResourcePropertyLookup lookup = session.getMapping().getResourcePropertyLookup(name);
        return new DefaultCompassQueryFilter(
                filterBuilder.gt(lookup.getPath(), lookup.getValue(value)));
    }

    public CompassQueryFilter ge(String name, Object value) {
        ResourcePropertyLookup lookup = session.getMapping().getResourcePropertyLookup(name);
        return new DefaultCompassQueryFilter(
                filterBuilder.ge(lookup.getPath(), lookup.getValue(value)));
    }

    public CompassQueryFilter query(CompassQuery query) {
        return new DefaultCompassQueryFilter(
                filterBuilder.query(((DefaultCompassQuery) query).getSearchEngineQuery()));
    }

    public CompassBooleanQueryFilterBuilder bool() {
        return new DefaultCompassBooleanQueryFilterBuilder(filterBuilder.bool());
    }
}
