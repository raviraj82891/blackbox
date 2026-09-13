package com.example.blackbox.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class NetworkDiagnosticTest {

    @Test
    fun testClassifyDnsFailure() {
        val exception = UnknownHostException("Unable to resolve host \"api.blackbox-safety.aws\"")
        val result = NetworkDiagnostic.classifyException(exception)

        assertTrue(result is NetworkDiagnosticResult.DnsFailure)
        assertTrue(result.getDiagnosticSummary().contains("DNS Failure"))
    }

    @Test
    fun testClassifyConnectionFailure() {
        val connException = ConnectException("Failed to connect to /192.168.1.1:443")
        val result1 = NetworkDiagnostic.classifyException(connException)

        assertTrue(result1 is NetworkDiagnosticResult.ConnectionFailure)
        assertTrue(result1.getDiagnosticSummary().contains("Connection Failure"))

        val timeoutException = SocketTimeoutException("timeout")
        val result2 = NetworkDiagnostic.classifyException(timeoutException)

        assertTrue(result2 is NetworkDiagnosticResult.ConnectionFailure)
    }

    @Test
    fun testClassifyTlsFailure() {
        val tlsException = SSLException("Certificate pinning failure!")
        val result = NetworkDiagnostic.classifyException(tlsException)

        assertTrue(result is NetworkDiagnosticResult.TlsFailure)
        assertTrue(result.getDiagnosticSummary().contains("TLS Failure"))
    }

    @Test
    fun testClassifyHttpErrorCodes() {
        val res404 = NetworkDiagnostic.classifyHttpResponse(404, "Not Found")
        assertTrue(res404 is NetworkDiagnosticResult.Http4xx)
        assertEquals("HTTP 404 Client Error: Not Found", res404.getDiagnosticSummary())

        val res503 = NetworkDiagnostic.classifyHttpResponse(503, "Service Unavailable")
        assertTrue(res503 is NetworkDiagnosticResult.Http5xx)
        assertEquals("HTTP 503 Server Error: Service Unavailable", res503.getDiagnosticSummary())
    }
}
