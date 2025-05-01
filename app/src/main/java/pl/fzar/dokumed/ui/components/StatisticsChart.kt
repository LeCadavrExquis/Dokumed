import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import pl.fzar.dokumed.data.model.BarData
import pl.fzar.dokumed.data.model.PieSliceData
import pl.fzar.dokumed.data.model.PointData
import pl.fzar.dokumed.data.model.ScatterPointData
import pl.fzar.dokumed.data.model.StatisticsChartData

@Composable
fun StatisticsChart(
    chartData: StatisticsChartData,
    modifier: Modifier = Modifier
) {
    when (chartData) {
        is StatisticsChartData.LineChart -> {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()  // Pełna szerokość
                    .height(250.dp), // Ograniczenie wysokości wykresu
                factory = { context ->
                    createLineChart(context, chartData.points)
                }
            )
        }
        is StatisticsChartData.BarChart -> {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()  // Pełna szerokość
                    .height(250.dp), // Ograniczenie wysokości wykresu
                factory = { context ->
                    createBarChart(context, chartData.bars)
                }
            )
        }
        is StatisticsChartData.PieChart -> {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp),
                factory = { context -> createPieChart(context, chartData.slices) },
//                update = { chart -> updatePieChart(chart, chartData.slices) }
            )
        }
        is StatisticsChartData.ScatterChart -> {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .height(300.dp),
                factory = { context -> createScatterChart(context, chartData.points) },
//                update = { chart -> updateScatterChart(chart, chartData.points) }
            )
        }
    }
}

private fun createLineChart(context: Context, points: List<PointData>): LineChart {
    val chart = LineChart(context)
    Log.d("LineChart", "Creating line chart with ${points.size} points")
    // Ustawienia wykresu
    chart.layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )

    val entries = points.mapIndexed { index, point ->
        Entry(index.toFloat(), point.y.toFloat())
    }

    val dataSet = LineDataSet(entries, "Dane")
    dataSet.setDrawValues(false)
    dataSet.setDrawCircles(true)
    dataSet.lineWidth = 2f

    chart.data = LineData(dataSet)
    chart.description = Description().apply { text = "" }
    chart.xAxis.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val idx = value.toInt()
            return points.getOrNull(idx)?.x ?: ""
        }
    }

    chart.axisRight.isEnabled = false

    // Dodajemy ograniczenie zakresu Y oraz innych elementów
    chart.axisLeft.axisMinimum = 0f
    chart.xAxis.granularity = 1f
    chart.xAxis.isGranularityEnabled = true
    chart.xAxis.setDrawGridLines(false)
    chart.axisLeft.setDrawGridLines(false)
    chart.xAxis.labelRotationAngle = -45f
    chart.legend.isEnabled = false
    chart.setTouchEnabled(false)
    chart.setPinchZoom(false)

    chart.invalidate()  // Odświeżenie wykresu
    return chart
}

private fun createBarChart(context: Context, bars: List<BarData>): BarChart {
    val chart = BarChart(context)

    val entries = bars.mapIndexed { index, bar ->
        BarEntry(index.toFloat(), bar.value.toFloat())
    }

    val dataSet = BarDataSet(entries, "Dane")
    dataSet.setDrawValues(true)

    val data = com.github.mikephil.charting.data.BarData(dataSet)
    chart.data = data
    chart.description = Description().apply { text = "" }
    chart.xAxis.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val idx = value.toInt()
            return bars.getOrNull(idx)?.label ?: ""
        }
    }

    chart.axisRight.isEnabled = false
    chart.invalidate()  // Odświeżenie wykresu
    return chart
}

private fun createPieChart(context: Context, slices: List<PieSliceData>): PieChart {
    val chart = PieChart(context)
    chart.layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    chart.description = Description().apply { text = "" }
    chart.isDrawHoleEnabled = true
    chart.setUsePercentValues(true)
    chart.setEntryLabelTextSize(12f)
    chart.centerText = "Types"
    chart.setCenterTextSize(16f)
    chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
    chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
    chart.legend.orientation = Legend.LegendOrientation.VERTICAL
    chart.legend.setDrawInside(false)
    chart.legend.isEnabled = true
//    updatePieChart(chart, slices)
    return chart
}

private fun createScatterChart(context: Context, points: List<ScatterPointData>): ScatterChart {
    val chart = ScatterChart(context)
    chart.layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    )
    chart.description.isEnabled = false
    chart.legend.isEnabled = false
    chart.setTouchEnabled(true)
    chart.setPinchZoom(true)
    chart.axisRight.isEnabled = false
    chart.xAxis.setDrawGridLines(false)
    chart.axisLeft.setDrawGridLines(true)
    chart.axisLeft.axisMinimum = 0f
//    updateScatterChart(chart, points)
    return chart
}

@Preview(showBackground = true)
@Composable
fun PreviewStatisticsChart() {
    val sampleLineData = StatisticsChartData.LineChart(
        points = List(10) { index ->
            PointData(
                x = "Point $index",
                y = (Math.random() * 100)
            )
        }
    )

    val sampleBarData = StatisticsChartData.BarChart(
        bars = List(5) { index ->
            BarData(
                label = "Bar $index",
                value = (Math.random() * 100)
            )
        }
    )

    val samplePieData = StatisticsChartData.PieChart(
        slices = List(8) { index ->
            PieSliceData(
                label = "Slice $index",
                value = (Math.random() * 100).toFloat()
            )
        }
    )

    val sampleScatterData = StatisticsChartData.ScatterChart(
        points = List(15) { index ->
            ScatterPointData(
                x = (Math.random() * 100).toFloat(),
                y = (Math.random() * 100).toFloat()
            )
        }
    )

    Column {
        StatisticsChart(
            chartData = sampleLineData,
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )

        StatisticsChart(
            chartData = sampleBarData,
            modifier = Modifier.fillMaxWidth().height(250.dp)
        )

        StatisticsChart(
            chartData = samplePieData,
            modifier = Modifier.fillMaxWidth().height(300.dp)
        )

        StatisticsChart(
            chartData = sampleScatterData,
            modifier = Modifier.fillMaxWidth().height(300.dp)
        )
    }
}
