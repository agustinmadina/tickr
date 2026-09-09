package dev.madina.tickr.feature.portfolio.ui

import org.jetbrains.compose.resources.StringResource
import tickr.features.feature_portfolio.ui.generated.resources.Res
import tickr.features.feature_portfolio.ui.generated.resources.message_catalogue_unavailable
import tickr.features.feature_portfolio.ui.generated.resources.message_generic_failure
import tickr.features.feature_portfolio.ui.generated.resources.message_numbers_required
import tickr.features.feature_portfolio.ui.generated.resources.message_pick_an_asset

/**
 * Something the ViewModel needs to tell the user, named rather than written out.
 *
 * The ViewModel decides *that* there is a message; the resource file decides how it reads. That
 * split is what makes the app translatable without touching the ViewModel, and it is why the
 * enum carries a [StringResource] rather than the copy itself.
 *
 * It also replaced passing `Throwable.message` straight to a snackbar, which put a
 * stack-trace-flavoured sentence in front of a user, in whatever language the library that threw
 * it happened to be written in. That belongs in the log, where it now goes.
 */
internal enum class UiMessage(
    val resource: StringResource,
) {
    PickAnAssetFirst(Res.string.message_pick_an_asset),
    NumbersRequired(Res.string.message_numbers_required),
    CatalogueUnavailable(Res.string.message_catalogue_unavailable),
    GenericFailure(Res.string.message_generic_failure),
}
