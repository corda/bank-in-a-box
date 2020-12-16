package com.r3.refapp.repositories

import javax.persistence.EntityManager
import javax.persistence.Query

/**
 * Construct and return a parametrized [Query] from the query String [baseQueryString].
 * [baseQueryString] is extended for not null values in [queryParams] using predicates in [paramToPredicateMap].
 * @param queryParams [Map] of parameter name and value pairs
 * @param paramToPredicateMap [Map] of parameter name and predicate pairs used to extend [baseQueryString]
 * @param baseQueryString base query [String] to construct parametrized query from
 * @return parametrized [Query] result list
 */
fun EntityManager.getParametrizedQuery(
        queryParams: Map<String, Any?>,
        paramToPredicateMap: Map<String, String>,
        baseQueryString: String,
        sortColumnAndOrder: String? = ""): Query {

    val notNullParams = queryParams.filterValues { it != null }

    val queryStr = baseQueryString +
            notNullParams.map { paramToPredicateMap[it.key] }.joinToString(" ") +
            (if (sortColumnAndOrder != "") " ORDER BY $sortColumnAndOrder" else "")

    val query = this.createQuery(queryStr)

    queryParams.forEach {
        if (queryStr.contains(":${it.key}")) {
            query.setParameter(it.key, it.value)
        }
    }

    return query
}