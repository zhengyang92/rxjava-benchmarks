package co.smartreceipts.android.tooltip

import co.smartreceipts.android.di.scopes.FragmentScope
import co.smartreceipts.android.tooltip.model.StaticTooltip
import co.smartreceipts.android.tooltip.privacy.PrivacyPolicyTooltipController
import co.smartreceipts.android.tooltip.rating.AppRatingTooltipController
import javax.inject.Inject
import javax.inject.Provider

/**
 * Manages the process of mapping a given [StaticTooltip] to a [TooltipController] implementation
 */
@FragmentScope
class TooltipControllerProvider @Inject constructor(private val privacyPolicyTooltipProvider: Provider<PrivacyPolicyTooltipController>,
                                                    private val appRatingTooltipProvider: Provider<AppRatingTooltipController>) {

    /**
     * Fetches the appropriate [TooltipController] for a given [StaticTooltip]
     *
     * @param tooltip the [StaticTooltip] to determine if we should display
     * @return the corresponding [TooltipController]
     */
    fun get(tooltip: StaticTooltip): TooltipController {
        return when (tooltip) {
            StaticTooltip.PrivacyPolicy -> privacyPolicyTooltipProvider.get()
            StaticTooltip.RateThisApp -> appRatingTooltipProvider.get()
        }
    }
}