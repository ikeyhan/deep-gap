package ir.atom313.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConfigDto(
    val site: SiteDto = SiteDto(),
    val commerce: CommerceDto = CommerceDto(),
    val support: SupportConfigDto = SupportConfigDto(),
    val registrationOpen: Boolean = true,
    val maintenance: Boolean = false,
    val android: AndroidConfigDto = AndroidConfigDto(),
)

@Serializable
data class SiteDto(
    val name: String = "اتم ۳۱۳",
    val description: String = "",
    val domain: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val social: SocialDto = SocialDto(),
)

@Serializable
data class SocialDto(val instagram: String = "", val telegram: String = "", val whatsapp: String = "", val linkedin: String = "")

@Serializable
data class CommerceDto(
    val shippingCost: Long = 0,
    val freeShippingMin: Long = 0,
    val minOrder: Long = 0,
    val onlinePayment: Boolean = false,
    val paymentNote: String = "",
)

@Serializable
data class SupportConfigDto(val chatEnabled: Boolean = true, val aiEnabled: Boolean = false, val title: String = "", val welcome: String = "")

@Serializable
data class AndroidConfigDto(val minVersionCode: Int = 1, val latestVersionCode: Int = 1)
