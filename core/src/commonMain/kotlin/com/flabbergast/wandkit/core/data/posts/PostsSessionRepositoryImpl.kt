package com.flabbergast.wandkit.core.data.posts

import com.flabbergast.wandkit.core.config.AppConfiguration
import com.flabbergast.wandkit.core.data.networking.WandKitApi
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionDeviceDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionRequestDto
import com.flabbergast.wandkit.core.data.posts.dto.SdkPostsSessionResponseDto
import com.flabbergast.wandkit.core.domain.infrastructure.logger.Logger
import com.flabbergast.wandkit.core.domain.posts.PostsConfig
import com.flabbergast.wandkit.core.domain.posts.PostsSession
import com.flabbergast.wandkit.core.domain.posts.PostsSessionRepository
import com.flabbergast.wandkit.core.domain.posts.PostsTag
import com.flabbergast.wandkit.core.platform.PlatformContext
import com.flabbergast.wandkit.core.platform.readDeviceContext

internal fun createPostsSessionRepository(
    postsApi: WandKitApi<PostsApi>,
    appConfiguration: AppConfiguration,
    platformContext: PlatformContext?,
    externalUserId: () -> String?,
    logger: Logger,
): PostsSessionRepository = PostsSessionRepositoryImpl(
    postsApi = postsApi,
    appConfiguration = appConfiguration,
    platformContext = platformContext,
    externalUserId = externalUserId,
    logger = logger,
)

private const val LOGGER_TAG = "[PostsSessionRepository]"

private class PostsSessionRepositoryImpl(
    private val postsApi: WandKitApi<PostsApi>,
    private val appConfiguration: AppConfiguration,
    private val platformContext: PlatformContext?,
    private val externalUserId: () -> String?,
    private val logger: Logger,
) : PostsSessionRepository {
    override suspend fun mintSession(): Result<PostsSession> {
        val deviceContext = readDeviceContext(platformContext)
        val request = SdkPostsSessionRequestDto(
            externalUserId = externalUserId()?.takeIf { it.isNotBlank() },
            device = SdkPostsSessionDeviceDto(
                platform = appConfiguration.platformName.lowercase(),
                osVersion = deviceContext.osVersion,
                appVersion = deviceContext.appVersion,
                deviceModel = deviceContext.deviceModel,
                locale = deviceContext.locale,
            ),
        )

        return postsApi { createSession(request) }
            .map { it.data.toDomain() }
            .onSuccess { logger.debug(LOGGER_TAG, "Minted posts session (readOnly=${it.readOnly})") }
            .onFailure { logger.debug(LOGGER_TAG, "Posts session mint failed: $it") }
    }
}

private fun SdkPostsSessionResponseDto.toDomain() = PostsSession(
    token = token,
    expiresAt = expiresAt,
    readOnly = readOnly,
    config = PostsConfig(
        enabledTypes = config.enabledTypes,
        roadmapEnabled = config.roadmapEnabled,
        tags = config.tags.map { PostsTag(id = it.id, name = it.name, color = it.color) },
    ),
)
