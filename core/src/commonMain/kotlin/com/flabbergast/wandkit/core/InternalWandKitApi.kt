package com.flabbergast.wandkit.core

/**
 * Marks API that is public only so WandKit's own modules (`ui-compose`) can
 * reach it. Not meant for host apps; it can change without notice.
 */
@RequiresOptIn(
    message = "Internal WandKit API, public only for WandKit's own modules. It can change without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalWandKitApi
