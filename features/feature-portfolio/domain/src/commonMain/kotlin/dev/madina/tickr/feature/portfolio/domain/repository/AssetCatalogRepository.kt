package dev.madina.tickr.feature.portfolio.domain.repository

import dev.madina.tickr.feature.portfolio.domain.model.Asset

interface AssetCatalogRepository {
    /**
     * Everything the exchange quotes against USD.
     *
     * This is what makes a holding priceable: an asset outside this list would be added to the
     * portfolio and then never receive a quote, showing as blank for ever with no explanation.
     */
    suspend fun tradableAssets(): List<Asset>
}
