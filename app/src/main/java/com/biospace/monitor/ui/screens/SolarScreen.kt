package com.biospace.monitor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biospace.monitor.model.SpaceWeatherState

@Composable
fun SolarScreen(sw: SpaceWeatherState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DashboardScreen(sw)
        ImfScreen(sw)
        CmeScreen(sw.cmeEvents)
        SolarImagesScreen()
    }
}
