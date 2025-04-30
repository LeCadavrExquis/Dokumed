package pl.fzar.dokumed.ui.statistics

import StatisticsChart
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.*
import pl.fzar.dokumed.ui.components.FilterBottomSheet
import pl.fzar.dokumed.util.toEpochMilliseconds
import pl.fzar.dokumed.util.toLocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    selectedType: MedicalRecordType?,
    selectType: (MedicalRecordType?) -> Unit,
    metric: StatisticMetric,
    selectMetric: (StatisticMetric) -> Unit,
    dateFrom: LocalDate?,
    dateTo: LocalDate?,
    chartData: StatisticsChartData?,
    updateDateRange: (LocalDate?, LocalDate?) -> Unit,
    onBack: () -> Unit
) {
    var chartType by remember { mutableStateOf("auto") } // "auto", "line", "bar"
    var showSummary by remember { mutableStateOf(true) }

    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = dateFrom?.toEpochMilliseconds(),
        initialSelectedEndDateMillis = dateTo?.toEpochMilliseconds()
    )

    val bottomSheetState = rememberModalBottomSheetState()
    var isExpanded by remember { mutableStateOf(false) }

    // Zarządzanie stanem rozwijania ModalBottomSheet
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            bottomSheetState.expand()
        } else {
            bottomSheetState.hide()
        }
    }

    // Śledzenie zmiany zakresu dat
    LaunchedEffect(dateRangePickerState.selectedStartDateMillis, dateRangePickerState.selectedEndDateMillis) {
        val from = dateRangePickerState.selectedStartDateMillis?.toLocalDate()
        val to = dateRangePickerState.selectedEndDateMillis?.toLocalDate()
        updateDateRange(from, to)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // TopAppBar without being in Scaffold
        TopAppBar(
            title = { Text(stringResource(R.string.statistics_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
        
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isExpanded) {
                ModalBottomSheet(
                    onDismissRequest = { isExpanded = false },
                    sheetState = bottomSheetState
                ) {
                    FilterBottomSheet(
                        allTypes = MedicalRecordType.entries.toList(),
                        selectedTypes = selectedType?.let { setOf(it) } ?: emptySet(),
                        onTypesChange = { set ->
                            selectType(set.firstOrNull())
                        },
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        onDateRangeChange = updateDateRange,
                        allTags = emptyList(), // TODO: Provide tags from ViewModel if needed
                        selectedTags = emptySet(), // TODO: Provide selected tags from ViewModel if needed
                        onTagsChange = {}, // TODO: Provide handler if needed
                        onDismiss = { isExpanded = false }
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
            ) {
                // Podsumowanie danych
                if (showSummary && chartData != null) {
                    StatisticsSummary(chartData)
                }
                // Wykres
                if (chartData != null) {
                    val displayChartData = when (chartType) {
                        "bar" -> if (chartData is StatisticsChartData.BarChart) chartData else null
                        "line" -> if (chartData is StatisticsChartData.LineChart) chartData else null
                        else -> chartData
                    }
                    if (displayChartData != null) {
                        StatisticsChart(
                            chartData = displayChartData,
                            modifier = Modifier.fillMaxWidth().height(250.dp)
                        )
                    } else {
                        Text(stringResource(R.string.no_chart_for_type))
                    }
                } else {
                    Text(stringResource(R.string.no_data))
                }
                // Przycisk "Filtry"
                if (!isExpanded) {
                    Box(
                        Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Button(
                            onClick = { isExpanded = true },
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(0.5f) // pół szerokości ekranu
                        ) {
                            Text(stringResource(R.string.filters))
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun PreviewStatisticsScreen() {
    StatisticsScreen(
        selectedType = null,
        selectType = {},
        metric = StatisticMetric.COUNT_OVER_TIME,
        selectMetric = {},
        dateFrom = null,
        dateTo = null,
        chartData = StatisticsChartData.LineChart(
            points = List(10) { index ->
                PointData(
                    x = "2025-04-${index + 1}",
                    y = (Math.random() * 100)
                )
            }
        ),
        updateDateRange = { _, _ -> },
        onBack = {}
    )
}