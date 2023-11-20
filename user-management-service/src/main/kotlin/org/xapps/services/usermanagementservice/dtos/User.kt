package org.xapps.services.usermanagementservice.dtos

import com.fasterxml.jackson.annotation.JsonProperty

data class User(
    var id: Long = 0,
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var password: String? = null,
    var roles: List<Role>? = null
)