package org.xapps.services.usermanagementservice.daos.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object UsersRoles : Table(name = "users_roles") {
    val user = reference(name = "user", foreign = Users, onDelete = ReferenceOption.CASCADE)
    val role = reference(name = "role", foreign = Roles, onDelete = ReferenceOption.CASCADE)

    override val primaryKey = PrimaryKey(user, role)
}