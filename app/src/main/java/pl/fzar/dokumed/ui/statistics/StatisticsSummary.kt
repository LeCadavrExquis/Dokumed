package pl.fzar.dokumed.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.fzar.dokumed.R
import pl.fzar.dokumed.data.model.StatisticsChartData

@Composable
fun StatisticsSummary(chartData: StatisticsChartData) {
    val stats = when (chartData) {
        is StatisticsChartData.LineChart -> chartData.points.map { it.y }
        is StatisticsChartData.BarChart -> chartData.bars.map { it.value }
        is StatisticsChartData.PieChart -> chartData.slices.map { it.value.toDouble() }
        is StatisticsChartData.ScatterChart -> chartData.points.map { it.y.toDouble() }
    }
    if (stats.isEmpty() || stats.all { it == 0.0 }) {
        Text(
            text = stringResource(R.string.no_data),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    val min = stats.minOrNull() ?: 0.0
    val max = stats.maxOrNull() ?: 0.0
    val avg = if (stats.isNotEmpty()) stats.average() else 0.0
    val count = stats.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = stringResource(R.string.summary_min, min), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.summary_max, max), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.summary_avg, avg), style = MaterialTheme.typography.bodyMedium)
        Text(text = stringResource(R.string.summary_count, count), style = MaterialTheme.typography.bodyMedium)
    }
}
