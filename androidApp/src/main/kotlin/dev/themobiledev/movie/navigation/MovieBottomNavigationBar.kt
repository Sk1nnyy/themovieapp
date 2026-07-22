package dev.themobiledev.movie.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.themobiledev.movie.R

private data class BottomNavTab(
    val route: MovieRoute,
    val labelRes: Int,
    val icon: String,
)

private val bottomNavTabs = listOf(
    BottomNavTab(MovieRoute.PopularMoviesList, R.string.tab_popular, "🎬"),
    BottomNavTab(MovieRoute.Favorites, R.string.tab_favorites, "⭐"),
)

@Composable
fun MovieBottomNavigationBar(
    currentRoute: MovieRoute?,
    onTabSelected: (MovieRoute) -> Unit,
) {
    NavigationBar {
        bottomNavTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onTabSelected(tab.route) },
                icon = { Text(text = tab.icon) },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}
