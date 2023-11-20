package org.xapps.services.usermanagementservice.daos.tables

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable(name = "users") {
    val firstName = varchar("first_name", 250)
    val lastName = varchar("last_name", 250)
    val email = varchar("email", 250)
    val password = varchar("password", 512)
}

class UserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(Users)

    var firstName by Users.firstName
    var lastName by Users.lastName
    var email by Users.email
    var password by Users.password
    var roles by RoleEntity via UsersRoles
}