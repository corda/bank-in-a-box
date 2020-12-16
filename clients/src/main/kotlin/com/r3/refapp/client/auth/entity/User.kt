package com.r3.refapp.client.auth.entity

import org.hibernate.annotations.Type
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.Table

/**
 * OAuth2 User entity.
 */
@Entity
@Table(name = "user", schema = "public")
class User(

        /**
         * Username
         */
        @Id
        @Column(name = "username")
        private var username: String,

        /**
         * User's email
         */
        @Column(name = "email")
        var email: String,

        /**
         * User's password
         */
        @Column(name = "password")
        private var password: String,

        /**
         * User account enabled flag
         */
        @Column(name = "enabled")
        var enabled: Boolean,

        /**
         * User account locked flag
         */
        @Column(name = "account_locked")
        var accountNonLocked: Boolean,

        /**
         * User account expired flag
         */
        @Column(name = "account_expired")
        var accountNonExpired: Boolean,

        /**
         * User account credentials expired flag
         */
        @Column(name = "credentials_expired")
        var credentialsNonExpired: Boolean,

        /**
         * User account to role mapping
         */
        @ManyToMany(fetch = FetchType.EAGER)
        @JoinTable(name = "role_user", joinColumns = [JoinColumn(name = "user_id", referencedColumnName = "username")],
                inverseJoinColumns = [JoinColumn(name = "role_id", referencedColumnName = "id")])
        var roles: MutableList<Role> = mutableListOf(),

        /**
         * Customer's "Bank in a box" Id
         */
        @Column(name = "customer_id", nullable = true, unique = true)
        @Type(type = "uuid-char")
        var customerId: UUID?,

        /**
         * Customer's "Bank in a box" attachment
         */
        @Column(name = "attachment_hash", nullable = true)
        var attachment: String?

) : UserDetails {

    override fun getAuthorities(): Set<GrantedAuthority> {
        return roles.map { SimpleGrantedAuthority(it.name) }.toSet()
    }

    override fun isEnabled() = enabled

    override fun getUsername() = username

    override fun isCredentialsNonExpired() = !credentialsNonExpired

    override fun getPassword() = password

    override fun isAccountNonExpired() = !accountNonExpired

    override fun isAccountNonLocked() = !accountNonLocked

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false

        if (username != other.username) return false
        if (email != other.email) return false
        if (password != other.password) return false
        if (enabled != other.enabled) return false
        if (accountNonLocked != other.accountNonLocked) return false
        if (accountNonExpired != other.accountNonExpired) return false
        if (credentialsNonExpired != other.credentialsNonExpired) return false
        if (roles != other.roles) return false
        if (customerId != other.customerId ) return false
        if (attachment != other.attachment ) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + accountNonLocked.hashCode()
        result = 31 * result + accountNonExpired.hashCode()
        result = 31 * result + credentialsNonExpired.hashCode()
        result = 31 * result + roles.hashCode()
        result = 31 * result + (customerId?.hashCode() ?: 0)
        result = 31 * result + (attachment?.hashCode() ?: 0)
        return result
    }
}