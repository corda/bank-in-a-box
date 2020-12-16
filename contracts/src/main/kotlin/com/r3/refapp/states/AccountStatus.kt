package com.r3.refapp.states

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class AccountStatus {
    SUSPENDED, PENDING, ACTIVE
}

/**
 * Verifies that [AccountStatus] can progress to given [accountStatus].
 *
 * @param accountStatus [AccountStatus] to progress to
 * @return returns [Boolean] based on test
 */
fun AccountStatus.canProgressToStatus(accountStatus: AccountStatus) =
        when(this) {
            AccountStatus.SUSPENDED -> accountStatus == AccountStatus.ACTIVE
            AccountStatus.PENDING -> accountStatus == AccountStatus.ACTIVE || accountStatus == AccountStatus.SUSPENDED
            AccountStatus.ACTIVE -> accountStatus == AccountStatus.SUSPENDED
        }