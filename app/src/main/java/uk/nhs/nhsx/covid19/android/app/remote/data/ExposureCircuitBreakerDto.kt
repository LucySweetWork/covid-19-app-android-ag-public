package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerRequest(
    val matchedKeyCount: Int,
    val daysSinceLastExposure: Int,
    val maximumRiskScore: Double
)

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerResponse(
    val approvalToken: String,
    val approval: String
)

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerPollingResponse(
    val approval: String
)