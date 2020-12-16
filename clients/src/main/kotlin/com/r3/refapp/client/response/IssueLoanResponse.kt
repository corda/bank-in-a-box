package com.r3.refapp.client.response

import com.r3.refapp.states.CurrentAccountState
import com.r3.refapp.states.LoanAccountState

/** Class to store the issue loan payment endpoint response */
data class IssueLoanResponse(val currentAccount: CurrentAccountState, val loanAccount: LoanAccountState)