package com.example.blackbox.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.data.crypto.KeyManagementService
import com.example.blackbox.data.crypto.SignatureStatus
import com.example.blackbox.data.db.IncidentReport
import com.example.blackbox.data.repository.BlackboxRepository
import com.example.blackbox.domain.pdf.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repository: BlackboxRepository,
    private val keyManagementService: KeyManagementService,
    private val pdfGenerator: PdfReportGenerator
) : ViewModel() {

    val incidentReports: StateFlow<List<IncidentReport>> = repository.getAllIncidentReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uploadingReportIds = MutableStateFlow<Set<String>>(emptySet())
    val uploadingReportIds: StateFlow<Set<String>> = _uploadingReportIds.asStateFlow()

    fun verifyIncidentIntegrity(report: IncidentReport): Boolean {
        val sig = report.digitalSignature ?: return false
        val pubKey = report.publicKeyBase64 ?: return false
        return keyManagementService.verifySignature(report.chainRootHash, sig, pubKey)
    }

    fun getSignatureStatus(report: IncidentReport): SignatureStatus {
        val sig = report.digitalSignature
        val pubKey = report.publicKeyBase64
        if (sig.isNullOrBlank() || pubKey.isNullOrBlank()) {
            return SignatureStatus.UNSIGNED
        }
        val isValid = keyManagementService.verifySignature(report.chainRootHash, sig, pubKey)
        return if (isValid) SignatureStatus.SIGNED else SignatureStatus.SIGNATURE_INVALID
    }

    fun uploadIncident(report: IncidentReport) {
        if (_uploadingReportIds.value.contains(report.id)) return // Prevent duplicate uploads
        viewModelScope.launch {
            _uploadingReportIds.value = _uploadingReportIds.value + report.id
            try {
                val contacts = repository.getAllEmergencyContacts().stateIn(viewModelScope).value
                val enabledIds = contacts.filter { it.isEnabled }.map { it.id }
                repository.uploadIncidentToBackend(report, enabledIds)
            } finally {
                _uploadingReportIds.value = _uploadingReportIds.value - report.id
            }
        }
    }

    fun generatePdfReport(report: IncidentReport): File {
        return pdfGenerator.generatePdfReport(report)
    }
}
