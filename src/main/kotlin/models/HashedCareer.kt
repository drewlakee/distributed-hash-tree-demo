package models

data class HashedCareer(
    val hashCode: Int,
    val item: CareerRecord? = null,
) : HashableEntity {
    override fun identity() = item?.id.toString()
    override fun isRemoved() = item == null
}
