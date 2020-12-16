package com.r3.refapp.repositories

import net.corda.core.serialization.CordaSerializable

/**
 * Query parameter class which holds a repository query's possible parameters.
 * @param startPage [Int] position of the result page to retrieve, starts from 1
 * @param pageSize [Int] specifies the maximum number of elements in a page
 * @param sortField [String] sort query output comparing this field
 * @param sortOrder [SortOrder] sorting order can be ascending or descending
 * @param searchTerm [String] fuzzy search terms for all fields
 */
@CordaSerializable
data class RepositoryQueryParams(val startPage: Int = 1, val pageSize: Int = 100,
                                 val sortField: String? = null, val sortOrder: SortOrder = SortOrder.ASC,
                                 val searchTerm: String = "") {
    @CordaSerializable
    enum class SortOrder {
        ASC, DESC
    }
}