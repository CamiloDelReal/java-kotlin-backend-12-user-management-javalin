package org.xapps.services.usermanagementservice.services

import java.util.*

class PropertiesService {
    private var properties: Properties? = null

    companion object {
        @JvmStatic
        val instance: PropertiesService by lazy {
            PropertiesService()
        }
    }

    init {
        try {
            val inputStream = ClassLoader.getSystemResourceAsStream("application.properties")
            properties = Properties()
            properties!!.load(inputStream)
        } catch (ex: Exception) {
            throw RuntimeException("Application properties file could not be loaded")
        }
    }

    val serverPort: Int
        get() = (properties!!["server.port"] as String).toInt()

    val databaseUrl: String
        get() = properties!!["database.url"] as String

    val databaseDriver: String
        get() = properties!!["database.driver"] as String

    val databaseUser: String
        get() = properties!!["database.user"] as String

    val databasePassword: String
        get() = properties!!["database.password"] as String


    val defaultRootFirstName: String
        get() = properties!!["defaults.root.first-name"] as String

    val defaultRootLastname: String
        get() = properties!!["defaults.root.last-name"] as String

    val defaultRootEmail: String
        get() = properties!!["defaults.root.email"] as String

    val defaultRootPassword: String
        get() = properties!!["defaults.root.password"] as String


    val securityHashRounds: Int
        get() = (properties!!["security.hash-rounds"] as String).toInt()

    val securitySecret: String
        get() = properties!!["security.secret"] as String

    val securityTokenType: String
        get() = properties!!["security.token-type"] as String

    val securityValidity: Long
        get() = (properties!!["security.validity"] as String).toLong()

    val securityIssuer: String
        get() = properties!!["security.issuer"] as String

    val securityAudience: String
        get() = properties!!["security.audience"] as String

    val secudityRealm: String
        get() = properties!!["security.realm"] as String
}
