package com.r3.refapp.util

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

/**
 * Utility data class used to map expected count of verification elements to type of verification element. Can be used
 * to create verification elements for commands, inputs, outputs .etc.
 */
data class VerificationElement<T>(val count: Int, val clazz: Class<T>)

/**
 * Creates [VerificationElement]. Utility extension infix function used to map element count and element type.
 * @param clazz [Class] of verification element
 * @return returns [VerificationElement]
 */
infix fun <T> Int.ofType(clazz: Class<T>) = VerificationElement(this, clazz)

/**
 * Creates [Pair] of account key name and key extractor function. Utility extension infix function used to map signer key
 * name and extractor function.
 * @param keyExtractor function used to extract signer's key
 * @return returns [Pair] of key name and extractor function
 */
infix fun String.key(keyExtractor: () -> PublicKey) = Pair(this, keyExtractor)

/**
 * Generic basic contract verification function.
 * Require that:
 *   There is total number of commands equal to sum of [commandVerificationElement]
 *   There is total number of inputs equal to sum of [inputVerificationElement]
 *   There is total number of outputs equal to sum of [outputVerificationElement]
 *   There is total number of referenced states equal to sum of [referencedStates]
 *   For each given command in [commandVerificationElement] verifies that number of commands of given type is equal
 *   to given count
 *   For each given input in [inputVerificationElement] verifies that number of inputs of given type is equal
 *   to given count
 *   For each given output in [outputVerificationElement] verifies that number of outputs of given type is equal
 *   to given count
 *   For each given referenced state in [referencedStates] verifies that number of referenced states of given type is equal
 *   to given count
 *   For each given signer verifies that signer is listed in command's signer list
 *
 * @param tx the [LedgerTransaction]
 * @param commands list of command verification elements
 * @param inputs list of input verification elements
 * @param outputs list of output verification elements
 * @param signerKeys list of signer key names and extractor functions
 * @param referencedStates list of referenced state verification elements
 * @throws IllegalArgumentException if any of the verifications fails
 */
fun executeBasicContractVerification(
        tx: LedgerTransaction,
        commands: List<VerificationElement<out CommandData>>,
        inputs: List<VerificationElement<out ContractState>>,
        outputs: List<VerificationElement<out ContractState>>,
        signerKeys: List<Pair<String, () -> PublicKey>>,
        referencedStates: List<VerificationElement<out ContractState>> = emptyList()) {

    val totalNumberOfCommands = commands.sumBy { it.count }
    val allSigners = tx.commands.flatMap { it.signers }.toSet()
    val commandName = commands.joinToString { it.clazz.simpleName }
    val totalNumberOfInputs = inputs.sumBy { it.count }
    val totalNumberOfOutputs = outputs.sumBy { it.count }
    val totalNumberOfReferencedStates = referencedStates.sumBy { it.count }
    requireThat {

        "Number of commands should be equal to $totalNumberOfCommands for $commandName" using
                (totalNumberOfCommands == tx.commands.count())
        "Number of inputs should be equal to $totalNumberOfInputs for $commandName" using
                (totalNumberOfInputs == tx.inputs.count())
        "Number of outputs should be equal to $totalNumberOfOutputs for $commandName" using
                (totalNumberOfOutputs == tx.outputs.count())
        "Number of referenced states should be equal to $totalNumberOfReferencedStates for $commandName" using
                (totalNumberOfReferencedStates == tx.referenceStates.count())

        commands.forEach {
            "Number of commands of type ${it.clazz.simpleName} should be equal to ${it.count}" using
                    (it.count == tx.commandsOfType(it.clazz).count())
        }
        inputs.forEach {
            "Number of inputs of type ${it.clazz.simpleName} should be equal to ${it.count} for $commandName" using
                    (it.count == tx.inputsOfType(it.clazz).count())
        }
        outputs.forEach {
            "Number of outputs of type ${it.clazz.simpleName} should be equal to ${it.count} for $commandName" using
                    (it.count == tx.outputsOfType(it.clazz).count())
        }
        referencedStates.forEach {
            "Number of referenced states of type ${it.clazz.simpleName} should be equal to ${it.count} for $commandName" using
                    (it.count == tx.referenceInputRefsOfType(it.clazz).count())
        }
        signerKeys.forEach {
            "${it.first} key must be ${if (signerKeys.count() == 1) "only signer" else "in signers list"} for $commandName" using
                    (allSigners.contains(it.second()))
        }
        "All signers from commands must be present in signerKeys" using (allSigners == signerKeys.map { it.second() }.toSet())
    }
}