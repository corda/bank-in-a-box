package com.r3.refapp.client.auth.repository

import com.r3.refapp.client.auth.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA role repository.
 */
@Repository
interface RoleRepository : JpaRepository<Role, Long> {

    fun findByName(name: String): Role
}