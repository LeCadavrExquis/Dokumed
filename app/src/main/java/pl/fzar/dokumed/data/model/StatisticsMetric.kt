package pl.fzar.dokumed.data.model

import android.content.Context
import androidx.annotation.StringRes
import pl.fzar.dokumed.R

// Data classes for chart points/bars
data class PointData(val x: String, val y: Double)
data class BarData(val label: String, val value: Double)
data class PieSliceData(val label: String, val value: Float) // For PieChart
data class ScatterPointData(val x: Float, val y: Float) // For ScatterChart

// Sealed class to represent different chart types
sealed class StatisticsChartData {
    data class LineChart(val points: List<PointData>) : StatisticsChartData()
    data class BarChart(val bars: List<BarData>) : StatisticsChartData()
    data class PieChart(val slices: List<PieSliceData>) : StatisticsChartData() // New
    data class ScatterChart(val points: List<ScatterPointData>) : StatisticsChartData() // New
}


enum class StatisticMetric {
    COUNT_OVER_TIME, // Line chart
    AVG_MEASUREMENT, // Line chart (for MeasurementRecord)
    TYPE_DISTRIBUTION_BAR, // Bar chart
    TYPE_DISTRIBUTION_PIE, // Pie chart
    TOP_TAGS_BAR, // Bar chart
    MEASUREMENT_SCATTER // Scatter chart (for MeasurementRecord)
}

@StringRes
fun StatisticMetric.getStringResId(): Int {
    return when (this) {
        StatisticMetric.COUNT_OVER_TIME -> R.string.statistic_metric_count_over_time
        StatisticMetric.AVG_MEASUREMENT -> R.string.statistic_metric_avg_measurement
        StatisticMetric.TYPE_DISTRIBUTION_BAR -> R.string.statistic_metric_type_distribution
        StatisticMetric.TYPE_DISTRIBUTION_PIE -> R.string.statistic_metric_type_distribution
        StatisticMetric.TOP_TAGS_BAR -> R.string.statistic_metric_top_tags
        StatisticMetric.MEASUREMENT_SCATTER -> R.string.statistic_metric_measurement
    }
}

fun StatisticMetric.getLocalizedString(context: Context): String {
    return context.getString(this.getStringResId())
}