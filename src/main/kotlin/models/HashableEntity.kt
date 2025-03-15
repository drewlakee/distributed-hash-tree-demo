package models

sealed interface HashableEntity {
    fun identity(): String
    fun isRemoved(): Boolean
}