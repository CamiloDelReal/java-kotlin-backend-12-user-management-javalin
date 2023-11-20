package org.xapps.services.usermanagementservice.services

import at.favre.lib.crypto.bcrypt.BCrypt
import javalinjwt.JWTGenerator
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.xapps.services.usermanagementservice.daos.tables.RoleEntity
import org.xapps.services.usermanagementservice.daos.tables.UserEntity
import org.xapps.services.usermanagementservice.daos.tables.Users
import org.xapps.services.usermanagementservice.dtos.Login
import org.xapps.services.usermanagementservice.dtos.Role
import org.xapps.services.usermanagementservice.dtos.User
import org.xapps.services.usermanagementservice.exceptions.EmailNotAvailable
import org.xapps.services.usermanagementservice.exceptions.InvalidCredentialsException
import org.xapps.services.usermanagementservice.exceptions.NotFoundException

class UserService(
    private val database: Database,
    private val roleService: RoleService,
    private val propertiesService: PropertiesService
) {

    fun init() = transaction(
        db = database
    ) {
        SchemaUtils.create(Users)
    }

    fun seed() = transaction(
        db = database
    ) {
        if (UserEntity.count() == 0L) {
            val administratorRole = runBlocking {
                roleService.administrator()
            }
            UserEntity.new {
                firstName = propertiesService.defaultRootFirstName
                lastName = propertiesService.defaultRootLastname
                email = propertiesService.defaultRootEmail
                password = BCrypt.withDefaults().hashToString(
                    propertiesService.securityHashRounds,
                    propertiesService.defaultRootPassword.toCharArray()
                )
                roles = SizedCollection(listOf(administratorRole!!))
            }
        }
    }

    fun readAll(): List<User> = transaction(database) {
        UserEntity.all().with(UserEntity::roles)
            .map {
                User(
                    id = it.id.value,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    email = it.email,
                    password = null,
                    roles = it.roles.map { r ->
                        Role(
                            id = r.id.value,
                            name = r.name
                        )
                    }
                )
            }
            .toList()
    }

    fun read(id: Long): User = transaction(database) {
        UserEntity.findById(id)?.load(UserEntity::roles)?.let {
            User(
                id = it.id.value,
                firstName = it.firstName,
                lastName = it.lastName,
                email = it.email,
                password = null,
                roles = it.roles.map { r ->
                    Role(
                        id = r.id.value,
                        name = r.name
                    )
                }
            )
        } ?: run {
            throw NotFoundException("No user found with id $id")
        }
    }

    fun validateLogin(login: Login): User = transaction(database) {
        UserEntity.find { Users.email eq login.email }.with(UserEntity::roles).singleOrNull()?.let { user ->
            val result = BCrypt.verifyer().verify(login.password.toCharArray(), user.password)
            if (result.verified) {
                User(
                    id = user.id.value,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    password = user.password,
                    roles = user.roles.map { r -> Role(id = r.id.value, name = r.name) }
                )
            } else {
                throw InvalidCredentialsException("Invalid credentials")
            }
        } ?: run {
            throw InvalidCredentialsException("Invalid credentials")
        }
    }

    fun isAdministrator(user: User): Boolean {
        return user.roles?.any { r -> r.name == Role.ADMINISTRATOR } ?: false
    }

    fun hasAdministratorRole(user: User): Boolean {
        return isAdministrator(user)
    }

    fun create(user: User): User = transaction(database) {
        Users.select { Users.email eq user.email }.singleOrNull()?.let { emailDuplicated ->
            throw EmailNotAvailable("Email ${user.email} is not available")
        } ?: run {
            var newRoles: List<RoleEntity>? = null
            if (user.roles != null && user.roles?.isNotEmpty() == true) {
                newRoles = roleService.findByNames(user.roles!!.map(Role::name))
            }
            if (newRoles == null || newRoles.isEmpty()) {
                roleService.guest()?.let {
                    newRoles = listOf(it)
                }
            }
            val newUser = UserEntity.new {
                firstName = user.firstName
                lastName = user.lastName
                email = user.email
                password = BCrypt.withDefaults().hashToString(
                    propertiesService.securityHashRounds,
                    user.password!!.toCharArray()
                )
                roles = SizedCollection(newRoles!!)
            }
            User(
                id = newUser.id.value,
                firstName = newUser.firstName,
                lastName = newUser.lastName,
                email = newUser.email,
                password = null,
                roles = newUser.roles.map { r -> Role(id = r.id.value, name = r.name) }.toList()
            )
        }
    }

    fun update(id: Long, user: User): User = transaction(database) {
        Users.select { (Users.email eq user.email) and (Users.id neq id) }.singleOrNull()?.let { emailDuplicated ->
            throw EmailNotAvailable("Email ${user.email} is not available")
        } ?: run {
            val presentUser = UserEntity.findById(id)?.load(UserEntity::roles)
            if (presentUser != null) {
                presentUser.firstName = user.firstName
                presentUser.lastName = user.lastName
                presentUser.email = user.email
                user.password?.let {
                    presentUser.password = BCrypt.withDefaults().hashToString(
                        propertiesService.securityHashRounds,
                        it.toCharArray()
                    )
                }
                var newRoles: List<RoleEntity>? = null
                if (user.roles != null && user.roles?.isNotEmpty() == true) {
                    newRoles = roleService.findByNames(user.roles!!.map(Role::name))
                }
                if (newRoles != null && newRoles.isNotEmpty()) {
                    presentUser.roles = SizedCollection(newRoles)
                }
                presentUser.refresh(flush = true)

                User(
                    id = presentUser.id.value,
                    firstName = presentUser.firstName,
                    lastName = presentUser.lastName,
                    email = presentUser.email,
                    password = null,
                    roles = presentUser.roles.map { r -> Role(id = r.id.value, name = r.name) }.toList()
                )
            } else {
                throw NotFoundException("No user found with id $id")
            }
        }
    }

    fun delete(id: Long): Boolean = transaction(database) {
        UserEntity.findById(id)?.let {
            it.delete()
            true
        } ?: run {
            false
        }
    }

}