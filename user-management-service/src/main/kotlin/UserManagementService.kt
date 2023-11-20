import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.http.bodyAsClass
import javalinjwt.JWTGenerator
import javalinjwt.JWTProvider
import javalinjwt.JavalinJWT
import org.jetbrains.exposed.sql.Database
import org.xapps.services.usermanagementservice.dtos.Authentication
import org.xapps.services.usermanagementservice.dtos.Login
import org.xapps.services.usermanagementservice.dtos.Role
import org.xapps.services.usermanagementservice.dtos.User
import org.xapps.services.usermanagementservice.exceptions.EmailNotAvailable
import org.xapps.services.usermanagementservice.exceptions.InvalidCredentialsException
import org.xapps.services.usermanagementservice.exceptions.NotFoundException
import org.xapps.services.usermanagementservice.services.*
import java.util.*


fun main(args: Array<String>) {
    // Services initialization ----------------------------------------------
    val propertiesService = PropertiesService()
    val database = Database.connect(
        url = propertiesService.databaseUrl,
        driver = propertiesService.databaseDriver,
        user = propertiesService.databaseUser,
        password = propertiesService.databasePassword
    )
    val roleService = RoleService(
        database = database
    )
    val userService = UserService(
        database = database,
        propertiesService = propertiesService,
        roleService = roleService
    )
    val userRoleService = UserRoleService(
        database = database
    )
    val seederService = SeederService(
        roleService = roleService,
        userService = userService,
        userRoleService = userRoleService
    )


    // Seeding services -----------------------------------------------------
    seederService.seed()


    // Authentication/Authorization initialization ---------------------------
    val jsonMapper = ObjectMapper()

    val algorithm = Algorithm.HMAC256(propertiesService.securitySecret)
    val generator = JWTGenerator { user: User, alg: Algorithm ->
        val token: JWTCreator.Builder = JWT.create()
            .withIssuer(propertiesService.securityIssuer)
            .withSubject(jsonMapper.writeValueAsString(user))
            .withIssuedAt(Date(System.currentTimeMillis()))
            .withExpiresAt(Date(System.currentTimeMillis() + propertiesService.securityValidity))
            .withClaim("roles", user.roles?.map { it.name })
            .withClaim("id", user.id)
        token.sign(alg)
    }
    val verifier = JWT.require(algorithm).build()
    val provider = JWTProvider(algorithm, generator, verifier)


    // Context extensions to validate authorization -------------------------
    fun Context.validateAuthorizationGivenRoles(allowedRoles: Set<String>, continuation: () -> Unit) {
        val decodedJWT = JavalinJWT.getTokenFromHeader(this).flatMap(provider::validateToken)
        if (!decodedJWT.isPresent) {
            this.status(HttpStatus.UNAUTHORIZED).result("Missing or invalid token")
        } else {
            val roles = decodedJWT.get().getClaim("roles").asList(String::class.java)
            if (roles.intersect(allowedRoles).isNotEmpty()) {
                continuation.invoke()
            } else {
                this.status(HttpStatus.FORBIDDEN).result("Permission denied")
            }
        }
    }

    fun Context.validateAuthorizationForReadAndDelete(continuation: () -> Unit) {
        val decodedJWT = JavalinJWT.getTokenFromHeader(this).flatMap(provider::validateToken)
        if (!decodedJWT.isPresent) {
            this.status(HttpStatus.UNAUTHORIZED).result("Missing or invalid token")
        } else {
            val roles = decodedJWT.get().getClaim("roles").asList(String::class.java)
            if (roles.contains(Role.ADMINISTRATOR)) {
                continuation.invoke()
            } else {
                val id = this.pathParam("id").toLong()
                val tokenUserId = decodedJWT.get().getClaim("id").asLong()
                if (id == tokenUserId) {
                    continuation.invoke()
                } else {
                    this.status(HttpStatus.FORBIDDEN).result("Permission denied")
                }
            }
        }
    }

    fun Context.validateAuthorizationForCreateAndUpdate(
        continuation: (context: Context, id: Long?, user: User) -> Unit
    ) {
        val decodedJwt = JavalinJWT.getTokenFromHeader(this).flatMap(provider::validateToken)
        val user = this.bodyAsClass<User>()

        if (this.method() == HandlerType.POST) {
            if (
                !userService.hasAdministratorRole(user)
                || (decodedJwt.isPresent && decodedJwt.get().getClaim("roles").asList(String::class.java).contains(Role.ADMINISTRATOR))
            ) {
                continuation.invoke(this, null, user)
            } else {
                this.status(HttpStatus.FORBIDDEN).result("Permission denied")
            }
        } else {    // PUT
            val id = this.pathParam("id").toLong()
            if (
                decodedJwt.isPresent
                && (
                    (
                        id == decodedJwt.get().getClaim("id").asLong() && !userService.hasAdministratorRole(user)
                    )
                        || decodedJwt.get().getClaim("roles").asList(String::class.java).contains(Role.ADMINISTRATOR)
                    )
            ) {
                continuation.invoke(this, id, user)
            } else {
                this.status(HttpStatus.FORBIDDEN).result("Permission denied")
            }
        }
    }


    // Javalin initialization -----------------------------------------------
    val app = Javalin.create { config -> }
        .exception(Exception::class.java) { ex, ctx ->
            when (ex) {
                is InvalidCredentialsException -> ctx.status(HttpStatus.UNAUTHORIZED).result(ex.message!!.toByteArray())
                is EmailNotAvailable -> ctx.status(HttpStatus.CONFLICT).result(ex.message!!.toByteArray())
                is NotFoundException -> ctx.status(HttpStatus.NOT_FOUND).result(ex.message!!.toByteArray())
                else -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result(ex.message!!.toByteArray())
            }
        }
        .routes {
            path("login") {
                post { ctx ->
                    val login = ctx.bodyAsClass<Login>()
                    val user = userService.validateLogin(login)
                    val token = provider.generateToken(user)
                    val authentication = Authentication(
                        token = token
                    )
                    ctx.json(authentication).status(HttpStatus.OK)
                }
            }
            path("users") {
                get { ctx ->
                    ctx.validateAuthorizationGivenRoles(setOf(Role.ADMINISTRATOR)) {
                        ctx.json(userService.readAll()).status(HttpStatus.OK)
                    }
                }
                post { ctx ->
                    ctx.validateAuthorizationForCreateAndUpdate { context, id, user ->
                        val savedUser = userService.create(user)
                        ctx.json(savedUser).status(HttpStatus.CREATED)
                    }
                }
                path("{id}") {
                    get { ctx ->
                        ctx.validateAuthorizationForReadAndDelete {
                            val id = ctx.pathParam("id").toLong()
                            val user = userService.read(id)
                            ctx.json(user).status(HttpStatus.OK)
                        }
                    }
                    put { ctx ->
                        ctx.validateAuthorizationForCreateAndUpdate { context, id, user ->
                            val savedUser = userService.update(id!!, user)
                            ctx.json(savedUser).status(HttpStatus.OK)
                        }
                    }
                    delete { ctx ->
                        ctx.validateAuthorizationForReadAndDelete {
                            val id = ctx.pathParam("id").toLong()
                            userService.delete(id)
                            ctx.status(HttpStatus.OK)
                        }
                    }
                }
            }
        }.start(propertiesService.serverPort)
}