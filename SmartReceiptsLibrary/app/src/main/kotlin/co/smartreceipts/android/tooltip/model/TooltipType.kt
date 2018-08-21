package co.smartreceipts.android.tooltip.model

/**
 * Defines the different tooltip types that are available to us
 */
enum class TooltipType {

    /**
     * Indicates that this tooltip is displaying an informational message to the end user
     */
    Informational,

    /**
     * Indicates that this tooltip is asking an explicit question of this end user
     */
    Question,

    /**
     * Indicates that this tooltip is displaying an error message to the end user
     */
    Error,

}