package com.example.appteschi.testutil

import com.example.appteschi.service.AdminAccountAuthService
import com.example.appteschi.service.AdminAuthService
import com.example.appteschi.service.CuentaAuthService
import com.example.appteschi.service.LocalAccountAuthService
import com.example.appteschi.service.RemoteOtpService
import com.example.appteschi.service.SiiaAuth
import com.example.appteschi.service.TipoCuenta

/**
 * Fakes sin red real, compartidos entre AuthRepositoryTest y AuthViewModelTest,
 * para no duplicar la misma implementación en dos archivos.
 */

class FakeAdminAuthService(
    private val resultado: Result<AdminAccountAuthService.Account> =
        Result.failure(IllegalStateException("no configurado"))
) : AdminAuthService {
    var llamadas = 0
        private set

    override suspend fun autenticar(usuario: String, password: String): Result<AdminAccountAuthService.Account> {
        llamadas++
        return resultado
    }
}

class FakeCuentaAuthService(
    private val resultado: Result<LocalAccountAuthService.Account> =
        Result.failure(IllegalStateException("no configurado"))
) : CuentaAuthService {
    var llamadas = 0
        private set

    override suspend fun autenticar(matricula: String, password: String): Result<LocalAccountAuthService.Account> {
        llamadas++
        return resultado
    }
}

class FakeSiiaAuth(
    private val resultado: Result<Unit> = Result.failure(Exception("no configurado"))
) : SiiaAuth {
    var llamadas = 0
        private set

    override suspend fun validarCredenciales(usuario: String, password: String): Result<Unit> {
        llamadas++
        return resultado
    }
}

class FakeRemoteOtpService(
    private val resultadoEnviar: Result<Unit> = Result.success(Unit),
    private val resultadoVerificar: Result<String?> = Result.success(null)
) : RemoteOtpService {
    var llamadasEnviar = 0
        private set
    var llamadasVerificar = 0
        private set

    override suspend fun enviar(tipo: TipoCuenta, identificador: String, correo: String): Result<Unit> {
        llamadasEnviar++
        return resultadoEnviar
    }

    override suspend fun verificar(tipo: TipoCuenta, identificador: String, codigo: String): Result<String?> {
        llamadasVerificar++
        return resultadoVerificar
    }
}
