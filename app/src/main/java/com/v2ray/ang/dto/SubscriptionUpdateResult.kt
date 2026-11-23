package com.v2ray.ang.dto

/**
 * Specific failure reason for subscription or batch config import/refresh.
 */
enum class SubscriptionImportError {
    NONE,
    EMPTY_INPUT,
    INVALID_INPUT,
    HTTP_NOT_ALLOWED,
    FETCH_FAILED,
    NO_CONFIGS_FOUND,
    STORAGE_ERROR
}

/**
 * Structured outcome of an atomic batch config / subscription import operation.
 */
data class BatchImportResult(
    val directConfigCount: Int = 0,
    val subscriptionCount: Int = 0,
    val subscriptionConfigCount: Int = 0,
    val matchedSubscriptionId: String? = null,
    val refreshedExisting: Boolean = false,
    val error: SubscriptionImportError = SubscriptionImportError.NONE
) {
    val totalImportedConfigs: Int
        get() = directConfigCount + subscriptionConfigCount

    val isSuccess: Boolean
        get() = error == SubscriptionImportError.NONE &&
            (directConfigCount > 0 || subscriptionConfigCount > 0)
}

/**
 * Result of subscription update operation
 */
data class SubscriptionUpdateResult(
    val configCount: Int = 0,      // Total configs updated
    val successCount: Int = 0,     // Subscriptions updated successfully
    val failureCount: Int = 0,     // Subscriptions failed to update
    val skipCount: Int = 0,        // Subscriptions skipped (disabled)
    val lastError: SubscriptionImportError = SubscriptionImportError.NONE
) {
    /**
     * Combine two results by adding their counts
     */
    operator fun plus(other: SubscriptionUpdateResult): SubscriptionUpdateResult {
        val mergedError = when {
            other.lastError != SubscriptionImportError.NONE -> other.lastError
            else -> this.lastError
        }
        return SubscriptionUpdateResult(
            configCount = this.configCount + other.configCount,
            successCount = this.successCount + other.successCount,
            failureCount = this.failureCount + other.failureCount,
            skipCount = this.skipCount + other.skipCount,
            lastError = mergedError
        )
    }
}
