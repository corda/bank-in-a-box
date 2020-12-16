package com.r3.refapp.domain

import net.corda.core.serialization.CordaSerializable

/**
 * Paginated response data object which holds result of paginated query and pagination information.
 * @param result [List] generic list of query results
 * @param totalResults [Long] total count of results for query
 * @param pageSize [Int] number of results per page
 * @param pageNumber [Int] page number of the currently returned page (one based)
 * @param totalPages [Int] total number of pages which can be retrieved for query
 */
@CordaSerializable
data class PaginatedResponse<T>(val result: List<T>, val totalResults: Long, val pageSize: Int, val pageNumber: Int,
                                val totalPages: Int = getTotalPages(totalResults, pageSize)) {

    companion object {
        fun getTotalPages(totalResults: Long, pageSize: Int) : Int {
            return if(totalResults > 0) {
                val totalPages = totalResults / pageSize
                totalPages.toInt() + if (totalResults % pageSize == 0L) 0 else 1
            } else 0
        }
    }
}
