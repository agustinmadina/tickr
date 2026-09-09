package dev.madina.tickr

import androidx.compose.runtime.Composable
import dev.madina.tickr.core.ui.theme.TickrTheme
import dev.madina.tickr.di.appModules
import dev.madina.tickr.feature.portfolio.ui.PortfolioRoot
import org.koin.compose.KoinApplication

/**
 * The single entry point every platform renders. Android, iOS and web each host this and nothing
 * else, which is what keeps the three from drifting apart.
 *
 * Koin starts inside the composition rather than from each platform's launcher, so there is one
 * startup path instead of three, and no global state to initialise before the first frame.
 */
@Composable
fun App() {
    KoinApplication(application = { modules(appModules) }) {
        TickrTheme {
            PortfolioRoot()
        }
    }
}
