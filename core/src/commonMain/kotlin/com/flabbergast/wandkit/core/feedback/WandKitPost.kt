package com.flabbergast.wandkit.core.feedback

/** A feedback post as the current user sees it, from [com.flabbergast.wandkit.core.WandKit.post]. */
public data class WandKitPost(
    /** The post's public id. */
    public val id: String,
    public val title: String,
    /** The raw status value from the API: `open`, `under_review`, `planned`, `in_progress`, `done`, `wont_do` or `duplicate`. */
    public val status: String,
    public val voteCount: Int,
    public val viewerVoted: Boolean,
    /** False while no user is identified: wandkit takes votes only from identified users. */
    public val canVote: Boolean,
)

/** A post's vote state after [com.flabbergast.wandkit.core.WandKit.setVote]. */
public data class WandKitPostVote(
    public val voteCount: Int,
    public val viewerVoted: Boolean,
)
