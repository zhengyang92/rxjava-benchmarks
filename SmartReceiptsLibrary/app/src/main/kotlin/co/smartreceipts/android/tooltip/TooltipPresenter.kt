package co.smartreceipts.android.tooltip

import co.smartreceipts.android.analytics.Analytics
import co.smartreceipts.android.analytics.events.DataPoint
import co.smartreceipts.android.analytics.events.DefaultDataPointEvent
import co.smartreceipts.android.analytics.events.Events
import co.smartreceipts.android.di.scopes.FragmentScope
import co.smartreceipts.android.tooltip.model.StaticTooltip
import co.smartreceipts.android.tooltip.model.TooltipInteraction
import co.smartreceipts.android.utils.log.Logger
import co.smartreceipts.android.widget.mvp.BasePresenter
import co.smartreceipts.android.widget.mvp.Presenter
import com.hadisatrio.optional.Optional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import java.util.*
import javax.inject.Inject

/**
 * Implements the [Presenter] contract to display a [StaticTooltipView]
 */
@FragmentScope
class TooltipPresenter @Inject constructor(view: StaticTooltipView,
                                           private val tooltipControllerProvider: TooltipControllerProvider,
                                           private val analytics: Analytics) : BasePresenter<StaticTooltipView>(view) {

    private var activeTooltipController: TooltipController? = null

    override fun subscribe() {
        // Determine if we have a tooltip to display and show the highest priority one if so
        compositeDisposable.add(Single.fromCallable {
                    val tooltipSingles = ArrayList<Single<Optional<StaticTooltip>>>()
                    view.getSupportedTooltips().forEach {
                        tooltipSingles.add(tooltipControllerProvider.get(it).shouldDisplayTooltip())
                    }
                    return@fromCallable tooltipSingles
                }.flatMap { tooltipSingles ->
                    if (tooltipSingles.isNotEmpty()) {
                        return@flatMap Single.zip(tooltipSingles, { optionalTooltipsArrayAsObjects ->
                            var result = Optional.absent<StaticTooltip>()
                            optionalTooltipsArrayAsObjects.forEach {
                                @Suppress("UNCHECKED_CAST")
                                val optionalTooltip: Optional<StaticTooltip> = it as Optional<StaticTooltip>
                                if (optionalTooltip.isPresent) {
                                    if (!result.isPresent || optionalTooltip.get().priority > result.get().priority) {
                                        result = optionalTooltip
                                    }
                                }
                            }
                            return@zip result
                        })
                    } else {
                        // Don't zip an empty list
                        return@flatMap Single.just(Optional.absent<StaticTooltip>())
                    }
                }
                .filter {
                    return@filter it.isPresent
                }
                .map {
                    return@map it.get()
                }
                .doOnSuccess {
                    Logger.info(this, "Displaying tooltip: {}", it)
                    analytics.record(DefaultDataPointEvent(Events.Informational.DisplayingTooltip).addDataPoint(DataPoint("tooltip", it)))
                    this.activeTooltipController = tooltipControllerProvider.get(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { view.display(it) }
        )

        // Pre-configure our initial click to interaction mappings
        val tooltipInteractions = view.getTooltipClickStream().map { return@map TooltipInteraction.TooltipClick }
        val cancelButtonInteractions = view.getButtonCancelClickStream().map { return@map TooltipInteraction.CloseCancelButtonClick }
        val closeButtonInteractions = view.getCloseIconClickStream().map { return@map TooltipInteraction.CloseCancelButtonClick }
        val noButtonInteractions = view.getButtonNoClickStream().map { return@map TooltipInteraction.NoButtonClick }
        val yesButtonInteractions = view.getButtonYesClickStream().map { return@map TooltipInteraction.YesButtonClick }

        // And assemble these into an array
        val supportedInteractions = arrayListOf(tooltipInteractions, cancelButtonInteractions, closeButtonInteractions, noButtonInteractions, yesButtonInteractions)

        // Handle each click as appropriate for a given tooltip controller
        compositeDisposable.add(Observable.merge(supportedInteractions)
                .filter { _ ->
                    return@filter activeTooltipController != null
                }
                .flatMap {
                    return@flatMap activeTooltipController!!.handleTooltipInteraction(it).andThen(Observable.just(it))
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    // This consumer may be null here, so we use this as a safety mechanism
                    activeTooltipController?.consumeTooltipInteraction()?.accept(it)
                }
        )

    }
}
