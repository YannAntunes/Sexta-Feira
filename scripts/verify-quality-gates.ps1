<#
.SYNOPSIS
    Quality Gate Runner Oficial do Projeto Sexta-Feira.
.DESCRIPTION
    Executa verificacoes automatizadas que acompanham os documentos de governanca:
    1. Existencia e integridade dos arquivos Markdown oficiais (AGENTS.md, docs/*.md)
    2. Verificacao de encoding UTF-8 nos arquivos de configuracao
    3. Verificacao estatica de seguranca (Secret Scanning em application.properties)
    4. Execucao da suite de testes Python (sexta-feira-ia)
    5. Execucao da suite de testes Java/Spring Boot (sexta-feira-api com H2 isolado)
#>

param(
    # Desde a Etapa 3 o Secret Scanning e estrito por padrao (bloqueia).
    # -AllowLegacySecrets rebaixa para WARN; use apenas para diagnostico, nunca para concluir tarefa.
    [switch]$AllowLegacySecrets = $false
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot

$Failures = 0
$Warnings = 0

function Write-GateResult {
    param([string]$Gate, [string]$Status, [string]$Detail)
    if ($Status -eq "PASS") {
        Write-Host "[PASS] $Gate - $Detail" -ForegroundColor Green
    } elseif ($Status -eq "WARN") {
        Write-Host "[WARN] $Gate - $Detail" -ForegroundColor Yellow
        $script:Warnings++
    } else {
        Write-Host "[FAIL] $Gate - $Detail" -ForegroundColor Red
        $script:Failures++
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " SEXTA-FEIRA - VERIFICACAO AUTOMATIZADA DE QUALITY GATES" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------
# GATE 1: Documentacao de Governanca Obrigatoria
# ------------------------------------------------------------------
$RequiredDocs = @(
    "AGENTS.md",
    "README.md",
    "docs\PROJECT_AUDIT.md",
    "docs\ARCHITECTURE.md",
    "docs\CODING_STANDARDS.md",
    "docs\TESTING_STRATEGY.md",
    "docs\SECURITY.md",
    "docs\QUALITY_GATES.md",
    "docs\DEFINITION_OF_DONE.md",
    "docs\DEVELOPMENT_WORKFLOW.md"
)

$MissingDocs = @()
foreach ($doc in $RequiredDocs) {
    $fullPath = Join-Path $RepoRoot $doc
    if (-not (Test-Path $fullPath) -or ((Get-Item $fullPath).Length -lt 200)) {
        $MissingDocs += $doc
    }
}

if ($MissingDocs.Count -eq 0) {
    Write-GateResult "GATE-01 (Documentacao Oficial)" "PASS" "Todos os $($RequiredDocs.Count) documentos obrigatorios existem e possuem conteudo."
} else {
    Write-GateResult "GATE-01 (Documentacao Oficial)" "FAIL" "Documentos ausentes ou vazios: $($MissingDocs -join ', ')"
}

# ------------------------------------------------------------------
# GATE 2: Isolamento do Ambiente de Testes (Protecao do MySQL Local)
# ------------------------------------------------------------------
$TestPropsPath = Join-Path $RepoRoot "sexta-feira-api\src\test\resources\application.properties"
if (Test-Path $TestPropsPath) {
    $testPropsContent = Get-Content $TestPropsPath -Raw -Encoding UTF8
    if ($testPropsContent -match "jdbc:h2:mem:" -and $testPropsContent -match "spring\.task\.scheduling\.enabled=false") {
        Write-GateResult "GATE-02 (Isolamento de Banco de Testes)" "PASS" "Testes configurados com H2 in-memory e schedulers desativados."
    } else {
        Write-GateResult "GATE-02 (Isolamento de Banco de Testes)" "FAIL" "application.properties de teste nao garante H2 in-memory ou schedulers desligados."
    }
} else {
    Write-GateResult "GATE-02 (Isolamento de Banco de Testes)" "FAIL" "Arquivo src/test/resources/application.properties ausente."
}

# ------------------------------------------------------------------
# GATE 3: Secret Scanning em Configuracoes (Seguranca)
# ------------------------------------------------------------------
$MainPropsPath = Join-Path $RepoRoot "sexta-feira-api\src\main\resources\application.properties"
$PlainSecretsFound = @()
if (Test-Path $MainPropsPath) {
    $lines = Get-Content $MainPropsPath -Encoding UTF8
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("#") -or [string]::IsNullOrWhiteSpace($trimmed)) { continue }
        if ($trimmed -match "^([\w\.\-]*(password|token|secret|api-key|apikey)[\w\.\-]*)\s*=\s*(.*)$") {
            $key = $Matches[1]
            $val = $Matches[3].Trim()
            if ([string]::IsNullOrEmpty($val)) { continue }
            if ($val -match '^\$\{[A-Z0-9_]+(:)?\}$') { continue }   # ${VAR} ou ${VAR:} sem default
            # Valor literal ou placeholder com default nao vazio (ex.: ${DB_PASSWORD:minhasenha}) = segredo exposto
            $PlainSecretsFound += $key
        }
    }
}

if ($PlainSecretsFound.Count -eq 0) {
    Write-GateResult "GATE-03 (Secret Scanning)" "PASS" "Nenhum segredo em texto puro detectado em application.properties."
} elseif ($AllowLegacySecrets) {
    Write-GateResult "GATE-03 (Secret Scanning)" "WARN" "Segredos hardcoded detectados (modo diagnostico -AllowLegacySecrets): $($PlainSecretsFound -join ', ')"
} else {
    Write-GateResult "GATE-03 (Secret Scanning)" "FAIL" "Segredos hardcoded detectados (bloqueado): $($PlainSecretsFound -join ', ')"
}

# ------------------------------------------------------------------
# GATE 4: Suite de Testes Automatizados Python (sexta-feira-ia)
# ------------------------------------------------------------------
$PyDir = Join-Path $RepoRoot "sexta-feira-ia"
$PyExe = Join-Path $PyDir "venv\Scripts\python.exe"
if (Test-Path $PyExe) {
    Push-Location $PyDir
    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $pyOut = & $PyExe -m unittest discover -s tests -v 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0) {
            Write-GateResult "GATE-04 (Testes Python - sexta-feira-ia)" "PASS" "Suite unittest executada com sucesso."
        } else {
            Write-Host $pyOut
            Write-GateResult "GATE-04 (Testes Python - sexta-feira-ia)" "FAIL" "Falha na suite de testes Python."
        }
    } finally {
        $ErrorActionPreference = $prevEAP
        Pop-Location
    }
} else {
    Write-GateResult "GATE-04 (Testes Python - sexta-feira-ia)" "FAIL" "Executavel Python do venv nao encontrado em $PyExe"
}

# ------------------------------------------------------------------
# GATE 5: Suite de Testes Automatizados Java (sexta-feira-api)
# ------------------------------------------------------------------
$JavaDir = Join-Path $RepoRoot "sexta-feira-api"
$MvnCmd = Join-Path $JavaDir "mvnw.cmd"
if (Test-Path $MvnCmd) {
    Push-Location $JavaDir
    $prevEAP = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $mvnOut = & $MvnCmd test -B 2>&1 | Out-String
        if ($LASTEXITCODE -eq 0 -and $mvnOut -match "BUILD SUCCESS") {
            Write-GateResult "GATE-05 (Testes Java - sexta-feira-api)" "PASS" "Maven test (H2 isolado) executado com BUILD SUCCESS."
        } else {
            Write-Host $mvnOut
            Write-GateResult "GATE-05 (Testes Java - sexta-feira-api)" "FAIL" "Falha no build ou nos testes Maven."
        }
    } finally {
        $ErrorActionPreference = $prevEAP
        Pop-Location
    }
} else {
    Write-GateResult "GATE-05 (Testes Java - sexta-feira-api)" "FAIL" "mvnw.cmd nao encontrado em $JavaDir"
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " RESUMO: Falhas = $Failures | Avisos = $Warnings" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

if ($Failures -gt 0) {
    exit 1
}
exit 0
