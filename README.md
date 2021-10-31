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

The plugin contains a `Ranking` document facet and a `ranking` schema which contains additional metadata useful for search ranking.

## ES mapping

A custom ES mapping is included and declares the `ranking:weight` field as a [Rank Feature](https://www.elastic.co/guide/en/elasticsearch/reference/master/rank-feature.html) type.

## PageProvider

The `ExtendedElasticSearchNxqlPageProvider` PageProvider leverages the `ranking:weight` Rank Feature. The query translated from NXQL is embedded in a boolean query must part while the rank feature is used in a should statement.

```json
{
  "query": {
    "bool": {
      "must": [
        {
          <query generated from NXQL>
        }
      ],
      "should": [
        {
          "rank_feature": {
            "field": "ranking:weight",
            "saturation": {
              "pivot": pivot_value
            },
            "boost": boost_value
          }
        }
      ]
    }
  }
}
```

The `pivot_value` and `boost_value` are configured in nuxeo.conf with the following properties:

```
elasticsearch.ranking.feature.saturation.pivot=5
elasticsearch.ranking.feature.saturation.boost=10
```

This default values will give the following score boost for the following `ranking:weight` values:
- 1   ->  0.9 
- 10  ->  5
- 100 ->  9

`ranking:weight` default value is `10`. Thus, individual documents can be promoted or demoted in search results simply by increasing or decreasing the value.

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
