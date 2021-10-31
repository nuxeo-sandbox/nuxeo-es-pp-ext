/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.es.ext.pp;

import org.elasticsearch.search.aggregations.Aggregation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.aggregate.AggregateFactory;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExtendedElasticSearchNxqlPageProvider extends ElasticSearchNxqlPageProvider {

    @Override
    public List<DocumentModel> getCurrentPage() {

        long t0 = System.currentTimeMillis();

        // use a cache
        if (currentPageDocuments != null) {
            return currentPageDocuments;
        }
        error = null;
        errorMessage = null;
        log.debug("Perform query for provider '{}': with pageSize={}, offset={}", this::getName,
                this::getMinMaxPageSize, this::getCurrentPageOffset);
        currentPageDocuments = new ArrayList<>();
        CoreSession coreSession = getCoreSession();
        if (query == null) {
            buildQuery(coreSession);
        }
        if (query == null) {
            throw new NuxeoException(String.format("Cannot perform null query: check provider '%s'", getName()));
        }
        // Build and execute the ES query
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        try {
            NxQueryBuilder nxQuery = getQueryBuilder(coreSession).nxql(query)
                    .offset((int) getCurrentPageOffset())
                    .limit(getLimit())
                    .addAggregates(buildAggregates());
            if (searchOnAllRepositories()) {
                nxQuery.searchOnAllRepositories();
            }
            nxQuery.useUnrestrictedSession(useUnrestrictedSession());

            List<String> highlightFields = getHighlights();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                nxQuery.highlight(highlightFields);
            }

            EsResult ret = ess.queryAndAggregate(nxQuery);
            DocumentModelList dmList = ret.getDocuments();
            currentAggregates = new HashMap<>(ret.getAggregates().size());
            for (Aggregate<Bucket> agg : ret.getAggregates()) {
                currentAggregates.put(agg.getId(), agg);
            }
            setResultsCount(dmList.totalSize());
            currentPageDocuments = dmList;
        } catch (QueryParseException e) {
            error = e;
            errorMessage = e.getMessage();
            log.warn(e.getMessage(), e);
        }

        // send event for statistics !
        fireSearchEvent(getCoreSession().getPrincipal(), query, currentPageDocuments, System.currentTimeMillis() - t0);

        return currentPageDocuments;
    }

    private List<AggregateEsBase<? extends Aggregation, ? extends Bucket>> buildAggregates() {
        ArrayList<AggregateEsBase<? extends Aggregation, ? extends Bucket>> ret = new ArrayList<>(
                getAggregateDefinitions().size());
        boolean skip = isSkipAggregates();
        for (AggregateDefinition def : getAggregateDefinitions()) {
            AggregateEsBase<? extends Aggregation, ? extends Bucket> agg = AggregateFactory.create(def,
                    getSearchDocumentModel());
            if (!skip || !agg.getSelection().isEmpty()) {
                // if we want to skip aggregates but one is selected, it has to be computed to filter the result set
                ret.add(AggregateFactory.create(def, getSearchDocumentModel()));
            }
        }
        return ret;
    }

    public NxQueryBuilder getQueryBuilder(CoreSession session) {
        getParameters();
        return new ExtendedNxQueryBuilder(getCoreSession());
    }

}
