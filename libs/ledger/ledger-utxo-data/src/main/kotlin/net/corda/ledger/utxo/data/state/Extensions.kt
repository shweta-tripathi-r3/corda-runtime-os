package net.corda.ledger.utxo.data.state

import net.corda.ledger.utxo.data.transaction.UtxoOutputInfoComponent
import net.corda.v5.base.util.uncheckedCast
import net.corda.v5.ledger.utxo.*
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.isSubclassOf

fun UtxoOutputInfoComponent.getEncumbranceGroup(): EncumbranceGroup? {
    return encumbrance?.let { EncumbranceGroupImpl(requireNotNull(encumbranceGroupSize), encumbrance) }
}

/**
 * Casts the current [ContractState] to the specified type.
 *
 * @param T The underlying type of the [ContractState].
 * @param type The type of the [ContractState] to cast.
 * @return Returns a new [ContractState] instance cast to the specified type.
 * @throws IllegalArgumentException if the current [ContractState] cannot be cast to the specified type.
 */
fun <T : ContractState> ContractState.cast(type: Class<T>): T {
    return if (javaClass.isAssignableFrom(type)) type.cast(this)
    else throw IllegalArgumentException("ContractState of type ${javaClass.canonicalName} cannot be cast to type ${type.canonicalName}.")

}

/**
 * Casts the current [TransactionState] to the specified type.
 *
 * @param T The underlying type of the [ContractState].
 * @param type The type of the [ContractState] to cast to.
 * @return Returns a new [TransactionState] instance cast to the specified type.
 * @throws IllegalArgumentException if the current [TransactionState] cannot be cast to the specified type.
 */
fun <T : ContractState> TransactionState<*>.cast(type: Class<T>): TransactionState<T> {
    return TransactionStateImpl(contractState.cast(type), notary, encumbrance)
}

/**
 * Casts the current [StateAndRef] to the specified type.
 *
 * @param T The underlying type of the [ContractState].
 * @param type The type of the [ContractState] to cast to.
 * @return Returns a new [StateAndRef] instance cast to the specified type.
 * @throws IllegalArgumentException if the current [StateAndRef] cannot be cast to the specified type.
 */
fun <T : ContractState> StateAndRef<*>.cast(type: Class<T>): StateAndRef<T> {
    return StateAndRefImpl(state.cast(type), ref)
}

/**
 * Filters a collection of [StateAndRef] and returns only the elements that match the specified [ContractState] type.
 *
 * @param T The underlying type of the [ContractState].
 * @param type The type of the [ContractState] to cast to.
 * @return Returns a collection of [StateAndRef] that match the specified [ContractState] type.
 */
fun <T : ContractState> Iterable<StateAndRef<*>>.filterIsContractStateInstance(type: Class<T>): List<StateAndRef<T>> {
    return filter { it.state.contractState.javaClass.isAssignableFrom(type) }.map { it.cast(type) }
}

/**
 * Gets all the non-abstract [Contract] classes from the current [ContractState], and any that have been applied to any
 * of the current [ContractState] implementations supertypes.
 *
 * @return Returns all the non-abstract [Contract] classes from the current [ContractState], and any that have been
 * applied to any of the current [ContractState] implementations supertypes.
 *
 * @throws IllegalArgumentException if the current [ContractState] has not been annotated with [BelongsToContract], or
 * if the current [ContractState] is not enclosed within a class that implements [Contract].
 */
fun ContractState.getContractClasses(): List<Class<out Contract>> {

    val result = mutableListOf<Class<out Contract>>()

    // The most derived subclass must belong to a contract, regardless of whether any of its supertypes do.
    val subClassContractClass = requireNotNull(javaClass.getContractClass()) {
        "Unable to infer Contract class. ${javaClass.canonicalName} is not annotated with @BelongsToContract, " +
                "or does not have an enclosing class which implements Contract"
    }

    result.add(subClassContractClass)

    // Find all the contract classes that apply to any of the current ContractState supertypes.
    javaClass.kotlin.allSuperclasses
        .filter { it.isSubclassOf(ContractState::class) }
        .mapNotNull { it.java.getContractClass() }
        .forEach { result.add(it) }

    return result.distinct()
}

/**
 * Gets the non-abstract [Contract] class from any [Class], or null if no non-abstract [Contract] class is found.
 *
 * @return Returns the non-abstract [Contract] class from any [Class], or null if no non-abstract [Contract] class is found.
 */
private fun Class<*>.getContractClass(): Class<out Contract>? {
    val annotation = getAnnotation(BelongsToContract::class.java)

    if (annotation != null && !annotation.value.isAbstract) {
        return annotation.value.java
    }

    val enclosingClass = enclosingClass

    if (enclosingClass != null
        && Contract::class.java.isAssignableFrom(enclosingClass)
        && !enclosingClass.kotlin.isAbstract
    ) {
        return uncheckedCast(enclosingClass)
    }

    return null
}
