package pl.fzar.dokumed.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import pl.fzar.dokumed.data.entity.MedicalRecordWithTags
import pl.fzar.dokumed.data.model.BarData
import pl.fzar.dokumed.data.model.MedicalRecordType
import pl.fzar.dokumed.data.model.PieSliceData
import pl.fzar.dokumed.data.model.PointData
import pl.fzar.dokumed.data.model.StatisticMetric
import pl.fzar.dokumed.data.model.StatisticsChartData
import pl.fzar.dokumed.data.repository.MedicalRecordRepository

class StatisticsViewModel(
    medicalRecordRepository: MedicalRecordRepository
) : ViewModel() {

    private val allRecordsFlow: StateFlow<List<MedicalRecordWithTags>> = medicalRecordRepository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedType = MutableStateFlow<MedicalRecordType?>(null)
    val selectedType: StateFlow<MedicalRecordType?> = _selectedType

    private val _metric = MutableStateFlow(StatisticMetric.COUNT_OVER_TIME)
    val metric: StateFlow<StatisticMetric> = _metric

    private val _dateFrom = MutableStateFlow<LocalDate?>(null)
    private val _dateTo = MutableStateFlow<LocalDate?>(null)
    val dateFrom: StateFlow<LocalDate?> = _dateFrom
    val dateTo: StateFlow<LocalDate?> = _dateTo

    private val _chartData: StateFlow<StatisticsChartData?> = combine(
        allRecordsFlow, _selectedType, _metric, _dateFrom, _dateTo
    ) { records, type, metric, from, to ->
        buildChartData(records, type, metric, from, to)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val chartData: StateFlow<StatisticsChartData?> = _chartData

    fun selectType(type: MedicalRecordType?) {
        _selectedType.value = type
    }

    fun selectMetric(metric: StatisticMetric) {
        _metric.value = metric
    }

    fun updateDateRange(from: LocalDate?, to: LocalDate?) {
        _dateFrom.value = from
        _dateTo.value = to
    }

    private fun buildChartData(
        records: List<MedicalRecordWithTags>,
        type: MedicalRecordType?,
        metric: StatisticMetric,
        from: LocalDate?,
        to: LocalDate?
    ): StatisticsChartData {
        val filtered = records.filter { rec ->
            (type == null || rec.medicalRecord.type == type) &&
                    (from == null || rec.medicalRecord.date >= from) &&
                    (to == null || rec.medicalRecord.date <= to)
        }
        return when (metric) {
            StatisticMetric.COUNT_OVER_TIME -> {
                val countsByDate = filtered.groupBy { it.medicalRecord.date }
                    .map { (date, recs) -> PointData(date.toString(), recs.size.toDouble()) }
                    .sortedBy { it.x }
                StatisticsChartData.LineChart(countsByDate)
            }
            StatisticMetric.AVG_MEASUREMENT -> {
                // TODO: Implement AVG_MEASUREMENT logic
                StatisticsChartData.LineChart(
                    points = TODO()
                )
            }
            StatisticMetric.TYPE_DISTRIBUTION_BAR -> {
                val countsByType = filtered.groupBy { it.medicalRecord.type.name }
                    .map { (type, recs) -> BarData(type, recs.size.toDouble()) }
                StatisticsChartData.BarChart(countsByType)
            }
            StatisticMetric.TYPE_DISTRIBUTION_PIE -> {
                val countsByType = filtered.groupBy { it.medicalRecord.type.name }
                    .map { (type, recs) -> PieSliceData(type, recs.size.toFloat()) }
                StatisticsChartData.PieChart(countsByType)
            }
            StatisticMetric.TOP_TAGS_BAR -> {
                val tagCounts = filtered.flatMap { it.tags }
                    .groupingBy { it.name }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .map { (tag, count) -> BarData(tag, count.toDouble()) }
                StatisticsChartData.BarChart(tagCounts)
            }
            StatisticMetric.MEASUREMENT_SCATTER -> {
                // TODO: Implement MEASUREMENT_SCATTER logic
                StatisticsChartData.ScatterChart(
                    points = TODO()
                )
            }
        }
    }
}
