package com.example.util

data class CountryCode(
  val code: String,
  val dialCode: String,
  val name: String,
  val flagEmoji: String
) {
  val displayName: String
    get() = "$flagEmoji $name ($dialCode)"

  val shortLabel: String
    get() = "$flagEmoji $dialCode"
}

object CountryCodeHelper {
  val defaultCountry = CountryCode("IN", "+91", "India", "🇮🇳")

  val countries = listOf(
    CountryCode("IN", "+91", "India", "🇮🇳"),
    CountryCode("US", "+1", "United States", "🇺🇸"),
    CountryCode("CA", "+1", "Canada", "🇨🇦"),
    CountryCode("GB", "+44", "United Kingdom", "🇬🇧"),
    CountryCode("AU", "+61", "Australia", "🇦🇺"),
    CountryCode("DE", "+49", "Germany", "🇩🇪"),
    CountryCode("FR", "+33", "France", "🇫🇷"),
    CountryCode("JP", "+81", "Japan", "🇯🇵"),
    CountryCode("SG", "+65", "Singapore", "🇸🇬"),
    CountryCode("AE", "+971", "United Arab Emirates", "🇦🇪"),
    CountryCode("BR", "+55", "Brazil", "🇧🇷"),
    CountryCode("MX", "+52", "Mexico", "🇲🇽"),
    CountryCode("ZA", "+27", "South Africa", "🇿🇦"),
    CountryCode("NG", "+234", "Nigeria", "🇳🇬"),
    CountryCode("CN", "+86", "China", "🇨🇳"),
    CountryCode("KR", "+82", "South Korea", "🇰🇷"),
    CountryCode("ID", "+62", "Indonesia", "🇮🇩"),
    CountryCode("PK", "+92", "Pakistan", "🇵🇰"),
    CountryCode("BD", "+880", "Bangladesh", "🇧🇩"),
    CountryCode("PH", "+63", "Philippines", "🇵🇭"),
    CountryCode("ES", "+34", "Spain", "🇪🇸"),
    CountryCode("IT", "+39", "Italy", "🇮🇹"),
    CountryCode("NL", "+31", "Netherlands", "🇳🇱"),
    CountryCode("CH", "+41", "Switzerland", "🇨🇭"),
    CountryCode("SE", "+46", "Sweden", "🇸🇪"),
    CountryCode("NZ", "+64", "New Zealand", "🇳🇿"),
    CountryCode("IE", "+353", "Ireland", "🇮🇪"),
    CountryCode("SA", "+966", "Saudi Arabia", "🇸🇦"),
    CountryCode("MY", "+60", "Malaysia", "🇲🇾"),
    CountryCode("VN", "+84", "Vietnam", "🇻🇳"),
    CountryCode("TH", "+66", "Thailand", "🇹🇭"),
    CountryCode("EG", "+20", "Egypt", "🇪🇬"),
    CountryCode("KE", "+254", "Kenya", "🇰🇪"),
    CountryCode("AR", "+54", "Argentina", "🇦🇷"),
    CountryCode("CO", "+57", "Colombia", "🇨🇴"),
    CountryCode("CL", "+56", "Chile", "🇨🇱"),
    CountryCode("PL", "+48", "Poland", "🇵🇱"),
    CountryCode("TR", "+90", "Turkey", "🇹🇷"),
    CountryCode("IL", "+972", "Israel", "🇮🇱"),
    CountryCode("NO", "+47", "Norway", "🇳🇴"),
    CountryCode("DK", "+45", "Denmark", "🇩🇰"),
    CountryCode("FI", "+358", "Finland", "🇫🇮"),
    CountryCode("AT", "+43", "Austria", "🇦🇹"),
    CountryCode("BE", "+32", "Belgium", "🇧🇪"),
    CountryCode("PT", "+351", "Portugal", "🇵🇹"),
    CountryCode("GR", "+30", "Greece", "🇬🇷")
  )

  fun findByDialCode(dialCode: String): CountryCode? {
    val clean = if (dialCode.startsWith("+")) dialCode else "+$dialCode"
    return countries.firstOrNull { it.dialCode == clean }
  }

  fun extractDialCode(fullPhoneNumber: String): Pair<CountryCode, String> {
    val trimmed = fullPhoneNumber.trim()
    for (country in countries.sortedByDescending { it.dialCode.length }) {
      if (trimmed.startsWith(country.dialCode)) {
        val remaining = trimmed.removePrefix(country.dialCode).trim()
        return Pair(country, remaining)
      }
    }
    return Pair(defaultCountry, trimmed.removePrefix(defaultCountry.dialCode).trim())
  }
}
