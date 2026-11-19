package no.nav.utbetaling.beregning.trekk.utils

import javax.sql.DataSource

import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using

object SqlUtils {
    inline fun <reified T : Any> Row.optionalOrNull(columnLabel: String): T? =
        runCatching {
            this.any(columnLabel) as? T
        }.getOrNull()

    inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val constructorPropertyNames = T::class.primaryConstructor?.parameters?.mapNotNull { it.name }
        val props = T::class.memberProperties.associateBy { it.name }
        val propertyNames = constructorPropertyNames ?: props.keys.toList()
        return propertyNames.associateWith {
            props[it]?.get(this)
        }
    }

    fun <A> DataSource.transaction(operation: (TransactionalSession) -> A): A =
        using(sessionOf(this, returnGeneratedKey = true)) { session ->
            session.transaction { tx ->
                operation(tx)
            }
        }
}

interface IdValueClass {
    val id: Long
}
