package com.r3.refapp.schemas

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties

abstract class AbstractEntityTest {

    /**
     * Clones provided generic [source] object. Utility method used for entity tests, clones provided object using
     * Kotlin's Reflection API.
     *
     * @param source Source object to clone
     */
    protected inline fun <reified T> clone(source: T) : T {
        val properties = (T::class as KClass<*>).memberProperties
        val constructor = T::class.constructors.first()
        val props = constructor.parameters.map { prop -> properties.single { it.name == (prop.name!!) }.getter.call(source) }
        val cloned = constructor.call(*props.toTypedArray())
        properties.forEach {
            if(it is KMutableProperty<*>) {
                val value = it.getter.call(source)
                it.setter.call(cloned, value)
            }
        }
        return cloned
    }

    /**
     * Verifies object by using [validationFunction] after each property has been changed. Utility method used for entity
     * equals and hashCode tests. Provided [entity1] and [entity2] should have all non equal properties.
     * Each property of cloned object will be set to [entity2] property value and [validationFunction] will be triggered.
     * [excludeProps] will be skipped.
     *
     * @param entity1 First entity for verification
     * @param entity2 Second entity for verification
     * @param excludeProps Objects properties to skip in verification
     * @param validationFunction Function used to validate objects state after property change
     */
    protected inline fun <reified T> verifyWithEachPropertyChanged(entity1: T,
                                              entity2: T, excludeProps: List<String> = emptyList(),
                                              validationFunction: (T, T) -> Unit) {

        val cloned = clone(entity1)
        val properties = (T::class as KClass<*>).memberProperties
        properties.forEach {
            val value1 = it.getter.call(entity1)
            val value2 = it.getter.call(entity2)
            if(it is KMutableProperty<*> && !excludeProps.contains(it.name)) {
                it.setter.call(cloned, value2)
                validationFunction.invoke(entity1, cloned)
                it.setter.call(cloned, value1)
            }
        }
    }
}