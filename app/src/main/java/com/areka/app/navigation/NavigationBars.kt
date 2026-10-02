package com.areka.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.areka.app.core.designsystem.StatChip

data class NavTabItem(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val MainNavTabs = listOf(
    NavTabItem(
        tab = MainTab.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    NavTabItem(
        tab = MainTab.LEARN,
        label = "Learn",
        selectedIcon = Icons.Filled.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_tab_learn"
    ),
    NavTabItem(
        tab = MainTab.PRACTICE,
        label = "Practice",
        selectedIcon = Icons.Filled.FitnessCenter,
        unselectedIcon = Icons.Outlined.FitnessCenter,
        testTag = "nav_tab_practice"
    ),
    NavTabItem(
        tab = MainTab.PROGRESS,
        label = "Progress",
        selectedIcon = Icons.Filled.Insights,
        unselectedIcon = Icons.Outlined.Insights,
        testTag = "nav_tab_progress"
    ),
    NavTabItem(
        tab = MainTab.YOU,
        label = "You",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_you"
    )
)

@Composable
fun ArekaV2BottomBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("v2_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        MainNavTabs.forEach { tabItem ->
            val isSelected = currentTab == tabItem.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tabItem.tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tabItem.selectedIcon else tabItem.unselectedIcon,
                        contentDescription = tabItem.label
                    )
                },
                label = {
                    Text(
                        text = tabItem.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag(tabItem.testTag)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArekaV2TopBar(
    title: String,
    streakDays: Int,
    points: Int,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            navigationIcon?.invoke()
        },
        actions = {
            if (streakDays > 0) {
                StatChip(
                    label = "streak",
                    value = "${streakDays}d",
                    icon = Icons.Default.LocalFireDepartment,
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            if (points > 0) {
                StatChip(
                    label = "pts",
                    value = "$points",
                    icon = Icons.Default.Bolt,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
    )
}
