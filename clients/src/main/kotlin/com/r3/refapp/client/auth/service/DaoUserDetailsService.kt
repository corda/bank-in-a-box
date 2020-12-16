package com.r3.refapp.client.auth.service

import com.r3.refapp.client.NodeRPCConnection
import com.r3.refapp.client.auth.Constants.ROLE_NAME_CUSTOMER
import com.r3.refapp.client.auth.Constants.ROLE_NAME_GUEST
import com.r3.refapp.client.auth.Constants.USERNAME_PROPERTY
import com.r3.refapp.client.auth.entity.User
import com.r3.refapp.client.auth.repository.RoleRepository
import com.r3.refapp.client.auth.repository.UserRepository
import com.r3.refapp.client.utils.generateAttachmentHash
import com.r3.refapp.flows.reports.GetCustomerByIdFlow
import com.r3.refapp.repositories.RepositoryQueryParams
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.authentication.AccountStatusUserDetailsChecker
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Provides services for OAuth2 user details management in DB.
 */
@Service(value = "userDetailsService")
class DaoUserDetailsService: UserDetailsService {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var rpc: NodeRPCConnection

    /**
     * OAuth2 required user loading implementation. Loads user from DB for given [username]
     * @param username
     * @return Spring security [UserDetails] object
     */
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
        AccountStatusUserDetailsChecker().check(user)
        return user
    }

    /**
     * Creates new user with GUEST role. Optional [customerId] and [attachment] fields are verified against "Bank in
     * a box" customer.
     * @param username Selected username
     * @param password Password
     * @param customerId (Optional) "Bank in a box" customer id
     * @param attachment (Optional) Attachment assigned to customer
     * @throws IllegalArgumentException if username is already taken, user with customerId is already registered or
     * provided documentation doesn't matches customer's documentation
     */
    fun saveNewUser(username: String, password: CharSequence, email: String, customerId: UUID?, attachment: MultipartFile?) {
        val roleGuest = roleRepository.findByName(ROLE_NAME_GUEST)
        val encodedPassword = BCryptPasswordEncoder().encode(password)

        if (userRepository.existsById(username)) {
            throw IllegalArgumentException("User with username: $username already exists, please select different username!")
        }

        val hash = attachment?.let {
            it.generateAttachmentHash()
        }

        customerId?.let {
            if (userRepository.existsByCustomerId(customerId)) {
                throw IllegalArgumentException("User with customerId already exists")
            }

            val customer = rpc.proxy.startFlow(::GetCustomerByIdFlow, customerId).returnValue.getOrThrow()
            if(!customer.attachments.map { it.attachmentHash }.contains(hash)) {
                throw IllegalArgumentException("Cannot create user, provided documentation not matching customers " +
                        "documentation!")
            }
        }
        val user = User(username, email, encodedPassword, true, false, false,
                false, mutableListOf(roleGuest), customerId, hash)
        userRepository.save(user)
    }

    /**
     * Adds [roleName] to user account for given [username]. Role list is configurable in DB and currently possible
     * options are GUEST, CUSTOMER and ADMIN. GUEST role is default role given to newly created user accounts. If
     * CUSTOMER role is granted to user account valid customerId and attachment must be associated with user.
     * @param username Username
     * @param roleName Granted role name
     * @throws IllegalArgumentException if granted role is CUSTOMER and customerId and attachment are not provided.
     */
    fun addRoleToUser(username: String, roleName: String) {
        val role = roleRepository.findByName(roleName)
        val user = userRepository.findByUsername(username)

        if (role.name == ROLE_NAME_CUSTOMER && (user.customerId == null || user.attachment == null)) {
            throw IllegalArgumentException("Cannot assign $ROLE_NAME_CUSTOMER role to user without customerId and " +
                    "attachment")
        }

        user.roles.add(role)
        userRepository.save(user)
    }

    /**
     * Revokes [roleName] from user account with given [username].
     * @param username Username
     * @param roleName Granted role name to be revoked
     * @throws IllegalArgumentException if provided [roleName] is not granted to user with [username]
     */
    fun revokeRole(username: String, roleName: String) {
        val role = roleRepository.findByName(roleName)
        val user = userRepository.findByUsername(username)

        if (!user.roles.contains(role)) {
            throw IllegalArgumentException("Cannot revoke $roleName role from user, user doesn't have $roleName role granted!")
        }

        user.roles.remove(role)
        userRepository.save(user)
    }

    /**
     * Fetches all users with given role [roleName], [RepositoryQueryParams.searchTerm] can be used to further filter
     * result set, search term will be LIKE matched against usernames.
     * @param roleName User's role
     * @param queryParams [RepositoryQueryParams] search object
     * @return Returns Spring's [Page] object of users.
     */
    fun getAllWithRole(roleName: String, queryParams: RepositoryQueryParams) : Page<User> {

        val sort = Sort.by(Sort.Order(Sort.Direction.fromString(queryParams.sortOrder.toString()), queryParams
                .sortField ?: USERNAME_PROPERTY))
        val pageable = PageRequest.of(queryParams.startPage - 1, queryParams.pageSize, sort)
        return userRepository.findAllByRolesNameAndUsernameContaining(roleName, queryParams.searchTerm, pageable)
    }


}