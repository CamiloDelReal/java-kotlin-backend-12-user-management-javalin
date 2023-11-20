package org.xapps.services.usermanagementservice.daos.tables

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Roles : LongIdTable(name = "roles") {
    val name = varchar("name", 50)
}

class RoleEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<RoleEntity>(Roles)

    var name by Roles.name
}