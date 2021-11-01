# Description

This repository contains a plugin that includes additional search capabilities for the Elasticsearch integration

# How to build

```
git clone https://github.com/nuxeo-sandbox/nuxeo-es-pp-ext
cd nuxeo-es-pp-ext
mvn clean install
```

# Features

## Ranking metadata

The plugin contains a `Ranking` document facet and a `ranking` schema which contains additional metadata useful for search ranking:
- `ranking:weight` value between 1 and 100, default to 10 
- `ranking:promoted_keywords` a string containing keywords for which the document should be promoted in search results
- `ranking:demoted_keywords` a string containing keywords for which the document should be demoted in search results

## ES mapping

A custom ES mapping is included and declares:
- `ranking:weight` as a [Rank Feature](https://www.elastic.co/guide/en/elasticsearch/reference/master/rank-feature.html) type.
- `ranking:promoted_keywords.fulltext` and `ranking:demoted_keywords.fulltext` as two analyzed text fields 

## PageProvider

The `ExtendedElasticSearchNxqlPageProvider` PageProvider leverages the `ranking:weight` Rank Feature. The query translated from NXQL is embedded in a boolean query must part while the rank feature is used in a should statement.

```json
{
  "query": {
    "bool": {
      "must": [
        {
        "boosting": {
          "positive": {
            "bool": {
              "must": [
                {
                  "query": "<query generated from NXQL>"
                }
              ],
              "should": [
                {
                  "simple_query_string": {
                    "query": "<fulltext_query>",
                    "fields": [
                      "ranking:promoted_keywords.fulltext^1.0"
                    ],
                    "analyzer": "fulltext",
                    "default_operator": "or",
                    "boost": "<elasticsearch.boost.keyword.query.positive.boost>"
                  }
                }
              ]
            }
          },
          "negative": 
            {
              "simple_query_string": {
                "query": "<fulltext_query>",
                "fields": [
                  "ranking:demoted_keywords.fulltext^1.0"
                ],
                "analyzer": "fulltext",
                "default_operator": "or",
                "boost": "<elasticsearch.boost.keyword.query.negative.boost>"
              }
            }
          }
        }
      ],
      "should": [
        {
          "rank_feature": {
            "field": "ranking:weight",
            "saturation": {
              "pivot": "<elasticsearch.ranking.feature.saturation.pivot>"
            },
            "boost": "<elasticsearch.ranking.feature.saturation.boost>"
          }
        }
      ]
    }
  }
}
```

`<fulltext_query>` value is taken from the page provider predicate on the fulltext field

The default values will give the following score boost for the following `ranking:weight` values:
- 1   ->  0.9 
- 10  ->  5
- 100 ->  9

# How to Use

- Add the `Ranking` Feature or `ranking` schema to your project document types.
- Configure your pageprovider to use the `ExtendedElasticSearchNxqlPageProvider` class.


# Known limitations
The [Linear Rank Feature](https://www.elastic.co/guide/en/elasticsearch/reference/7.15/query-dsl-rank-feature-query.html#rank-feature-query-linear) is not available in the elasticsearch client used by Nuxeo LTS2021. 

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration, and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-es-pp-ext)

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com)
