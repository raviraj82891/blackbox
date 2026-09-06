package com.example.blackbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.pdf.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repository: BlackboxRepository,
    private val pdfGenerator: PdfReportGenerator
) : ViewModel() {

    val incidentReports: StateFlow<List<IncidentReport>> = repository.getAllIncidentReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun uploadIncident(report: IncidentReport) {
        viewModelScope.launch {
            repository.uploadIncidentToBackend(report, emptyList())
        }
    }

    fun generatePdfReport(report: IncidentReport): File {
        return pdfGenerator.generatePdfReport(report)
    }
}
