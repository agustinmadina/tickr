package dev.madina.tickr.feature.portfolio.domain.usecase

import dev.madina.tickr.core.domain.usecase.UseCase
import dev.madina.tickr.feature.portfolio.domain.model.Asset
import dev.madina.tickr.feature.portfolio.domain.repository.AssetCatalogRepository
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Finds assets to add, from the set the exchange actually quotes and the user does not already own.
 *
 * Ranking is the point of doing this in the domain rather than filtering in the UI. A plain
 * `contains` puts "Wrapped Bitcoin" alongside "Bitcoin" for the query "bit", and buries an exact
 * symbol match under whatever happens to sort first. Order here is: exact symbol, then symbol
 * prefix, then name prefix, then anything containing the term.
 */
class SearchAssetsUseCase(
    private val assetCatalogRepository: AssetCatalogRepository,
    coroutineDispatcher: CoroutineDispatcher,
) : UseCase<SearchAssetsUseCase.Params, List<Asset>>(coroutineDispatcher) {
    data class Params(
        val query: String,
        /**
         * Symbols already in the portfolio, excluded from the results.
         *
         * Offering one would invite a duplicate, and adding a held symbol silently replaces the
         * existing position with a new quantity and cost, which is not what picking it from a list
         * of things to add looks like it should do.
         */
        val excludedSymbols: Set<String> = emptySet(),
        /**
         * Caps ranked matches only. Browsing with no query returns the whole catalogue.
         *
         * Past the exact and prefix matches, ranking degrades to "the query appears somewhere in
         * this string", and a fortieth result of that kind is not something anyone scrolls to. The
         * unfiltered list is different: it is not ranked, so a cap there is not a relevance
         * judgement, it is hiding rows already sitting in memory. It also made the sheet stop at an
         * arbitrary letter, which reads as the exchange listing forty coins.
         */
        val limit: Int = DefaultLimit,
    )

    override suspend fun execute(parameters: Params): List<Asset> {
        val excluded = parameters.excludedSymbols.map { it.uppercase() }.toSet()
        val catalog = assetCatalogRepository.tradableAssets().filterNot { it.symbol.uppercase() in excluded }
        val query = parameters.query.trim().lowercase()

        if (query.isEmpty()) {
            // With no query, lead with the assets most people are looking for rather than with
            // whatever the exchange happens to return first, which is alphabetical noise.
            val popular = Popular.mapNotNull { symbol -> catalog.firstOrNull { it.symbol == symbol } }
            return (popular + catalog).distinct()
        }

        return catalog
            .mapNotNull { asset -> rank(asset, query)?.let { asset to it } }
            .sortedWith(compareBy({ it.second }, { it.first.symbol.length }, { it.first.symbol }))
            .map { it.first }
            .take(parameters.limit)
    }

    private fun rank(asset: Asset, query: String): Int? {
        val symbol = asset.symbol.lowercase()
        val name = asset.name.lowercase()
        return when {
            symbol == query -> ExactSymbol
            symbol.startsWith(query) -> SymbolPrefix
            name.startsWith(query) -> NamePrefix
            symbol.contains(query) || name.contains(query) -> Contains
            else -> null
        }
    }
}

private const val ExactSymbol = 0
private const val SymbolPrefix = 1
private const val NamePrefix = 2
private const val Contains = 3
private const val DefaultLimit = 40

private val Popular = listOf("BTC", "ETH", "SOL", "XRP", "ADA", "DOGE", "AVAX", "LINK", "DOT", "MATIC")
