package com.areka.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationManager {
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Explicit backstack of NavRoutes
    private val backStack = mutableListOf<NavRoute>(NavRoute.HomeRoot)

    private val _currentRoute = MutableStateFlow<NavRoute>(NavRoute.HomeRoot)
    val currentRoute: StateFlow<NavRoute> = _currentRoute.asStateFlow()

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
        val root = when (tab) {
            MainTab.HOME -> NavRoute.HomeRoot
            MainTab.LEARN -> NavRoute.LearnRoot
            MainTab.PRACTICE -> NavRoute.PracticeRoot
            MainTab.PROGRESS -> NavRoute.ProgressRoot
            MainTab.YOU -> NavRoute.YouRoot
        }
        // When switching tabs, reset the stack to the tab root so tabs don't deeply tangle
        backStack.clear()
        backStack.add(root)
        _currentRoute.value = root
    }

    fun navigateTo(route: NavRoute) {
        if (backStack.lastOrNull() == route) return
        backStack.add(route)
        _currentRoute.value = route
        updateTabForRoute(route)
    }

    fun replaceTop(route: NavRoute) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
        backStack.add(route)
        _currentRoute.value = route
        updateTabForRoute(route)
    }

    /**
     * Handles back press.
     * Returns true if back was consumed by popping stack, or false if already at Home root.
     */
    fun handleBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            val previous = backStack.last()
            _currentRoute.value = previous
            updateTabForRoute(previous)
            return true
        } else if (_currentTab.value != MainTab.HOME) {
            // If at the root of another tab, back navigates to Home
            selectTab(MainTab.HOME)
            return true
        }
        return false
    }

    fun popToRoot() {
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        _currentRoute.value = backStack.firstOrNull() ?: NavRoute.HomeRoot
    }

    private fun updateTabForRoute(route: NavRoute) {
        when (route) {
            is NavRoute.HomeRoot -> _currentTab.value = MainTab.HOME
            is NavRoute.LearnRoot -> _currentTab.value = MainTab.LEARN
            is NavRoute.PracticeRoot -> _currentTab.value = MainTab.PRACTICE
            is NavRoute.ProgressRoot -> _currentTab.value = MainTab.PROGRESS
            is NavRoute.YouRoot, is NavRoute.Auth -> _currentTab.value = MainTab.YOU
            else -> { /* retain active tab */ }
        }
    }
}
