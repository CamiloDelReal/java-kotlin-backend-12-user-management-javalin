package org.xapps.services.usermanagementservice.services

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.xapps.services.usermanagementservice.daos.tables.RoleEntity
import org.xapps.services.usermanagementservice.daos.tables.Roles
import org.xapps.services.usermanagementservice.dtos.Role

class RoleService(
    private val database: Database
) {

    fun init() = transaction(
        db = database
    ) {
        SchemaUtils.create(Roles)
    }

    fun seed() = transaction(
        db = database
    ) {
        if (RoleEntity.count() == 0L) {
            RoleEntity.new {
                name = Role.ADMINISTRATOR
            }
            RoleEntity.new {
                name = Role.GUEST
            }
        }
    }

    fun readAll(): List<Role> =
        RoleEntity.all()
            .map {
                Role(
                    id = it.id.value,
                    name = it.name
                )
            }
            .toList()

    fun administrator(): RoleEntity? =
        RoleEntity.find {
            Roles.name eq Role.ADMINISTRATOR
        }.singleOrNull()

    fun guest(): RoleEntity? =
        RoleEntity.find {
            Roles.name eq Role.GUEST
        }.singleOrNull()

    fun findByNames(names: List<String>): List<RoleEntity> =
        RoleEntity.find {
            Roles.name inList names
        }.toList()

}