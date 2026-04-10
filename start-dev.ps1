# Script de inicialização — ERP Bustech com Supabase
# Execute no PowerShell: .\start-dev.ps1

# ── Carrega .env se existir ─────────────────────────────────────────────────
if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.+)$') {
            Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim()
        }
    }
    Write-Host "[OK] .env carregado" -ForegroundColor Cyan
} else {
    Write-Warning ".env nao encontrado. Copie .env.example para .env e preencha as credenciais."
}

# ── Validação ─────────────────────────────────────────────────
if (-not $env:DATABASE_URL) {
    Write-Error "DATABASE_URL nao definido em .env"
    exit 1
}
if (-not $env:DATABASE_PASSWORD) {
    Write-Error "DATABASE_PASSWORD nao definido em .env"
    exit 1
}

$env:JAVA_HOME   = "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
$env:SERVER_PORT = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8081" }

Write-Host "Backend: http://localhost:$env:SERVER_PORT" -ForegroundColor Green

mvn spring-boot:run "-Dspring.profiles.active=dev"
