package com.flabbergast.wandkit.core.domain.posts

internal interface PostsSessionRepository {
    /**
     * Mints a session for whichever user is currently identified (anonymous,
     * and therefore read-only, when none is). Failures are logged and returned,
     * never thrown: the caller decides between an error state and a retry.
     */
    suspend fun mintSession(): Result<PostsSession>
}

internal data class PostsSession(
    val token: String,
    /** ISO-8601, as received; the web app parses it. */
    val expiresAt: String,
    val readOnly: Boolean,
    val config: PostsConfig,
    /** The user's effective display name - their own if set, else the host's suggestion. */
    val displayName: String? = null,
)

internal data class PostsConfig(
    val enabledTypes: List<String>,
    val roadmapEnabled: Boolean,
    val tags: List<PostsTag>,
)

internal data class PostsTag(
    val id: String,
    val name: String,
    val color: String?,
)
