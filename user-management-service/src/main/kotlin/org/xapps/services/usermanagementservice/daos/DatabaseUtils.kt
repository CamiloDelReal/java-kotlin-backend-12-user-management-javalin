package org.xapps.services.usermanagementservice.daos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseUtils {

    suspend fun <T> queryBlock(
        dispatcher: CoroutineDispatcher,
        db: Database,
        block: suspend () -> T
    ): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

}