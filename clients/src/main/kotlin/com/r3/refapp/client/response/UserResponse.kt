package com.r3.refapp.client.response

import com.r3.refapp.client.auth.entity.User

/**
 * Extension function on [User] entity used to map entity to UI friendly DTO [UserResponse].
 * @return Returns mapped [UserResponse] object,
 */
fun User.toResponse() =
        UserResponse(this.username, this.email, this.roles.joinToString { it.name })

/**
 * General [User] wrapper for endpoint responses
 * @param username User's username
 * @param email User's email
 * @param roles User's granted roles
 */
data class UserResponse(val username: String, val email: String, val roles: String)