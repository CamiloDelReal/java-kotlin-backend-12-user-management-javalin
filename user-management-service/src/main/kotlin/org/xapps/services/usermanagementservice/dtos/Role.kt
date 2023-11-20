package org.xapps.services.usermanagementservice.dtos

data class Role(
    var id: Long? = 0,
    var name: String = ""
) {

    companion object {
        const val ADMINISTRATOR = "Administrator"
        const val GUEST = "Guest"
    }

}