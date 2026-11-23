package com.flabbergast.wandkit.core.feedback

/**
 * Returned as the failure of [com.flabbergast.wandkit.core.WandKit.submitPost] when no user is
 * identified: anonymous sessions can read posts but not write them. Call
 * [com.flabbergast.wandkit.core.WandKit.identify] first.
 */
public class WandKitReadOnlySessionException internal constructor() :
    Exception("No user is identified, so this session is read-only. Call WandKit.identify first.")
