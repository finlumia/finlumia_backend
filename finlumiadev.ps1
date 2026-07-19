#!/usr/bin/env pwsh
param(
    [switch]$up,
    [switch]$rebuild,
    [switch]$shell,
    [switch]$down,
    [switch]$status,
    [switch]$logs,
    [switch]$help
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Uso: ./finlumiadev.ps1 [-up|-rebuild|-shell|-down|-status|-logs]"
}

$flags = @($up, $rebuild, $shell, $down, $status, $logs) | Where-Object { $_ }
if ($help -or $flags.Count -eq 0) { Show-Usage; exit 0 }
if ($flags.Count -gt 1) { Write-Host "ERRO: informe apenas um comando."; Show-Usage; exit 1 }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$composeFile = "docker-compose.dev.yml"
$baseImage = "finlumia/dev:almalinux10"
$baseImageTar = "docker/bases/finlumia-dev-almalinux10.tar"
$baseImage2 = "finlumia/base:finlumia-dev-almalinux10module_java21"
$baseImageTar2 = "docker/bases/finlumia-dev-almalinux10module_java21.tar"

if (-not (Test-Path $composeFile)) {
    Write-Host "ERRO: arquivo $composeFile nao encontrado."
    exit 1
}

function Ensure-BaseImage {
    param([string]$image, [string]$tar)

    $exists = docker images --format "{{.Repository}}:{{.Tag}}" | Where-Object { $_ -eq $image }
    if ($exists) { return }

    if (-not (Test-Path $tar)) {
        Write-Host "ERRO: arquivo da imagem base nao encontrado: $tar"
        exit 1
    }

    $loadOutput = docker load -i $tar 2>&1
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha no docker load."; exit 1 }
    $loadOutput | ForEach-Object { Write-Host $_ }

    $exists = docker images --format "{{.Repository}}:{{.Tag}}" | Where-Object { $_ -eq $image }
    if ($exists) { return }

    $loadedImage = $loadOutput |
        Select-String "^Loaded image: " |
        ForEach-Object { $_.Line -replace "^Loaded image: ", "" } |
        Select-Object -First 1

    if (-not $loadedImage) {
        Write-Host "ERRO: nao foi possivel identificar a imagem carregada para retag."
        exit 1
    }

    docker tag $loadedImage $image
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha ao retaggear imagem base."; exit 1 }
}

function Ensure-BaseImages {
    Ensure-BaseImage -image $baseImage -tar $baseImageTar
    Ensure-BaseImage -image $baseImage2 -tar $baseImageTar2
}

if ($up) {
    Ensure-BaseImages
    docker compose -f $composeFile up -d db dev
    exit $LASTEXITCODE
}

if ($rebuild) {
    Ensure-BaseImages
    docker compose -f $composeFile up -d --force-recreate db dev
    exit $LASTEXITCODE
}

if ($shell) {
    docker compose -f $composeFile exec --user finlumia dev bash
    exit $LASTEXITCODE
}

if ($down) {
    docker compose -f $composeFile down
    exit $LASTEXITCODE
}

if ($status) {
    docker compose -f $composeFile ps
    exit $LASTEXITCODE
}

if ($logs) {
    docker compose -f $composeFile logs -f dev
    exit $LASTEXITCODE
}
