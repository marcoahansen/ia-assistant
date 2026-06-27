param(
    [string]$Models = "nomic-embed-text,llama3.2:3b"
)

Write-Host "=== Iniciando Ollama ===" -ForegroundColor Cyan

# Verifica se o Docker está rodando
if (-not (docker info 2>$null)) {
    Write-Host "Docker nao esta rodando. Inicie o Docker Desktop primeiro." -ForegroundColor Red
    exit 1
}

# Sobe o container
Write-Host "Subindo container do Ollama..." -ForegroundColor Yellow
docker compose -f "$PSScriptRoot\..\docker-compose.yml" up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro ao subir o container." -ForegroundColor Red
    exit 1
}

# Aguarda o serviço ficar pronto
Write-Host "Aguardando Ollama ficar pronto..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -Method Get -ErrorAction Stop
        $ready = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $ready) {
    Write-Host "Ollama nao respondeu apos 60 segundos." -ForegroundColor Red
    exit 1
}

Write-Host "Ollama esta pronto!" -ForegroundColor Green

# Faz o pull dos modelos
$modelList = $Models -split ","
foreach ($model in $modelList) {
    $model = $model.Trim()
    if ($model) {
        Write-Host "Baixando modelo: $model..." -ForegroundColor Yellow
        docker compose -f "$PSScriptRoot\..\docker-compose.yml" exec -T ollama ollama pull $model
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Modelo '$model' baixado com sucesso!" -ForegroundColor Green
        } else {
            Write-Host "Erro ao baixar modelo '$model'." -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== Setup concluido! ===" -ForegroundColor Cyan
Write-Host "Ollama rodando em http://localhost:11434" -ForegroundColor Green
Write-Host ""
Write-Host "Para testar o embedding:" -ForegroundColor Gray
Write-Host '  curl http://localhost:11434/api/embed -d "{\"model\": \"nomic-embed-text\", \"input\": \"teste\"}"' -ForegroundColor Gray
Write-Host ""
Write-Host "Para testar o chat:" -ForegroundColor Gray
Write-Host '  curl http://localhost:11434/api/chat -d "{\"model\": \"llama3.2:3b\", \"messages\": [{\"role\": \"user\", \"content\": \"Ola\"}]}"' -ForegroundColor Gray
