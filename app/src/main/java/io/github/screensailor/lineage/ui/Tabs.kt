package io.github.screensailor.lineage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab as MaterialTab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.screensailor.lineage.ui.demos.console.Console as ConsoleContent
import io.github.screensailor.lineage.ui.demos.echoes.Echoes as EchoesContent
import io.github.screensailor.lineage.ui.demos.paths.Paths as PathsContent
import io.github.screensailor.lineage.ui.demos.reflections.Reflections as ReflectionsContent

@Composable
fun Tabs() {
    var selected by remember { mutableStateOf(Tab.Reflections) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selected.ordinal,
            containerColor = Color.Black,
            contentColor = Color.White
        ) {
            Tab.entries.forEach { tab ->
                MaterialTab(
                    selected = tab == selected,
                    onClick = { selected = tab },
                    text = { Text(tab.name) }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            selected.Content()
        }
    }
}

private enum class Tab {
    Reflections,
    Paths,
    Echoes,
    Console;

    @Composable
    fun Content() = when (this) {
        Paths -> PathsContent()
        Reflections -> ReflectionsContent()
        Echoes -> EchoesContent()
        Console -> ConsoleContent()
    }
}
