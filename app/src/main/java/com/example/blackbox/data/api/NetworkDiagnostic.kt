package com.example.blackbox.data.api

import com.example.blackbox.BuildConfig
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

sealed class NetworkDiagnosticResult {
    object DnsFailure : NetworkDiagnosticResult()
    object ConnectionFailure : NetworkDiagnosticResult()
    object TlsFailure : NetworkDiagnosticResult()
    data class Http4xx(val code: Int, val message: String) : NetworkDiagnosticResult()
    data class Http5xx(val code: Int, val message: String) : NetworkDiagnosticResult()
    object Success : NetworkDiagnosticResult()

    fun getDiagnosticSummary(): String {
        return when (this) {
            is DnsFailure -> "DNS Failure: Unable to resolve host '${BuildConfig.CERT_PIN_DOMAIN}'"
            is ConnectionFailure -> "Connection Failure: Unable to reach server or socket timed out"
            is TlsFailure -> "TLS Failure: Secure handshake or certificate validation failed"
            is Http4xx -> "HTTP $code Client Error: $message"
            is Http5xx -> "HTTP $code Server Error: $message"
            is Success -> "Backend Network Connectivity Verified"
        }
    }
}

object NetworkDiagnostic {

    fun classifyException(e: Throwable): NetworkDiagnosticResult {
        return when (e) {
            is UnknownHostException -> NetworkDiagnosticResult.DnsFailure
            is SSLHandshakeException, is SSLException -> NetworkDiagnosticResult.TlsFailure
            is ConnectException, is SocketTimeoutException -> NetworkDiagnosticResult.ConnectionFailure
            else -> NetworkDiagnosticResult.ConnectionFailure
        }
    }

    fun classifyHttpResponse(code: Int, message: String): NetworkDiagnosticResult {
        return when (code) {
            in 200..299 -> NetworkDiagnosticResult.Success
            in 400..499 -> NetworkDiagnosticResult.Http4xx(code, message)
            in 500..599 -> NetworkDiagnosticResult.Http5xx(code, message)
            else -> NetworkDiagnosticResult.Http5xx(code, message)
        }
    }
}
