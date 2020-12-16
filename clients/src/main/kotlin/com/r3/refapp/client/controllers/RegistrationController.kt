package com.r3.refapp.client.controllers

import com.r3.refapp.client.auth.service.DaoUserDetailsService
import com.r3.refapp.client.response.UserResponse
import com.r3.refapp.client.response.toResponse
import com.r3.refapp.domain.PaginatedResponse
import com.r3.refapp.repositories.RepositoryQueryParams
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*


/**
 * Provides register user API endpoints.
 */
@RestController
@RequestMapping("/register")
class RegistrationController {

    @Autowired
    lateinit var daoUserDetailsService: DaoUserDetailsService

    /**
     * Registers a new user account with GUEST role assigned
     * @param username users unique identifier
     * @param password password of the user
     * @param email email of the user
     * @param customerId optional Id of the customer
     */
    @PostMapping("/guest", produces = ["application/json"])
    fun registerUserAccount(@RequestParam(value = "username") username: String,
                            @RequestParam(value = "password") password: CharSequence,
                            @RequestParam(value = "email") email: String,
                            @RequestParam(value = "customerId", required = false) customerId: UUID?,
                            @RequestParam(value = "file", required = false) file: MultipartFile?) {

        daoUserDetailsService.saveNewUser(username, password, email, customerId, file)
    }

    /**
    * Grants a new role to user
    * @param username users unique identifier
    * @param roleName name of the new role to be added
    */
    @PostMapping("/admin/addRole", produces = ["application/json"])
    fun addRoleToUser(@RequestParam(value = "username") username: String,
                      @RequestParam(value = "roleName") roleName: String) {

        daoUserDetailsService.addRoleToUser(username, roleName)
    }

    /**
     * Revokes role from user
     * @param username users unique identifier
     * @param roleName name of the role to be revoked
     */
    @PostMapping("/admin/revokeRole", produces = ["application/json"])
    fun revokeRole(@RequestParam(value = "username") username: String,
                   @RequestParam(value = "roleName") roleName: String) {

        daoUserDetailsService.revokeRole(username, roleName)
    }

    /**
     * Fetches all users with given [roleName]. Paginated response can be further filtered by the username in
     * [searchTerm]. Result set can be sorted by username and email fields.
     * @param roleName Name of the role
     * @param startPage position of the result page to retrieve
     * @param pageSize specifies the maximum number of customers in one page
     * @param searchTerm can be matched in LIKE fashion against username and email fields.
     * @param sortField sort result based on this field
     * @param sortOrder order of the sort (ASC or DESC)
     * @return [PaginatedResponse] of the [UserResponse] objects
     */
    @GetMapping("/admin/users", produces = ["application/json"])
    fun getUsersInRole(@RequestParam(value = "roleName") roleName: String, @RequestParam startPage: Int,
                       @RequestParam pageSize: Int, @RequestParam sortField: String?, @RequestParam sortOrder: RepositoryQueryParams.SortOrder?,
                       @RequestParam searchTerm: String?): PaginatedResponse<UserResponse> {

        val queryParam = RepositoryQueryParams(startPage, pageSize, sortField, sortOrder ?: RepositoryQueryParams
                .SortOrder.ASC, searchTerm ?: "")
        val users = daoUserDetailsService.getAllWithRole(roleName, queryParam)
        return PaginatedResponse(users.content.map { it.toResponse() }, users.totalElements, pageSize, startPage, users
                .totalPages)
    }
}