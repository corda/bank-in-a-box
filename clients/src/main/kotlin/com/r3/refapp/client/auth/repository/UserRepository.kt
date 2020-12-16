package com.r3.refapp.client.auth.repository

import com.r3.refapp.client.auth.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.transaction.Transactional

/**
 * JPA user repository.
 */
@Repository
@Transactional
interface UserRepository: PagingAndSortingRepository<User, String> {

    fun existsByCustomerId(customerId: UUID): Boolean

    fun findByUsername(username: String): User

    fun findAllByRolesNameAndUsernameContaining(roleName: String, username: String, pageable: Pageable): Page<User>
}