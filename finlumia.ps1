#!/usr/bin/env pwsh
param(
    [Parameter(Mandatory=$false, Position=0)]
    [string]$module,
    [switch]$all,
    [switch]$t,
    [switch]$c,
    [switch]$s,
    [switch]$local,
    [switch]$hom,
    [switch]$pro
)

$ErrorActionPreference = "Stop"
# Evita que saida em stderr de comandos nativos (docker, gradle) vire excecao fatal.
# O script ja valida sucesso/falha via $LASTEXITCODE apos cada chamada.
$PSNativeCommandUseErrorActionPreference = $false

$BASE_IMAGE = "finlumia/base:finlumia-dev-almalinux10module_java21"
$BASE_IMAGE_TAR = "docker/bases/finlumia-dev-almalinux10module_java21.tar"
$DEV_CONTAINER = "finlumiadev"

$HOST_PORTS = @{
    "configurator" = 28081
    "identify"     = 28083
    "movement"     = 28084
    "docs"         = 28082
    "document"     = 28085
}

$CONTAINER_PORTS = @{
    "configurator" = 28081
    "identify"     = 28083
    "movement"     = 28084
    "docs"         = 28082
    "document"     = 28085
}

$GRADLE_TASKS = @{
    "configurator" = ":configurator:bootJar"
    "identify" = ":identify:bootJar"
    "movement" = ":movement:bootJar"
    "docs" = ":docs:bootJar"
    "document" = ":document:bootJar"
}

$VALID_MODULES = @("configurator","identify","movement","docs","document")

if ($t -and $c) { Write-Host "ERRO: use -t ou -c."; exit 1 }
if (-not $t -and -not $c) { Write-Host "ERRO: informe -t ou -c."; exit 1 }
if ($s -and -not $t) { Write-Host "ERRO: -s so pode ser usado com -t."; exit 1 }
$profileCount = @($local, $hom, $pro | Where-Object { $_ }).Count
if ($profileCount -ne 1) { Write-Host "ERRO: informe exatamente um de -local, -hom ou -pro."; exit 1 }
if ($all -and $module) { Write-Host "ERRO: use modulo unico ou -all."; exit 1 }
if (-not $all -and -not $module) { Write-Host "ERRO: informe um modulo ou use -all."; exit 1 }
if (-not $all -and ($VALID_MODULES -notcontains $module)) {
    Write-Host "ERRO: modulo invalido. Use: $($VALID_MODULES -join ', ')"
    exit 1
}

$PROFILE = if ($local) { "local" } elseif ($hom) { "hom" } else { "pro" }
$TARGET_MODULES = if ($all) { $VALID_MODULES } else { @($module) }

function Ensure-BaseImage {
    $exists = docker images --format "{{.Repository}}:{{.Tag}}" | Where-Object { $_ -eq $BASE_IMAGE }
    if ($exists) { return }

    if (-not (Test-Path $BASE_IMAGE_TAR)) {
        Write-Host "ERRO: arquivo da imagem base nao encontrado: $BASE_IMAGE_TAR"
        exit 1
    }

    $loadOutput = docker load -i $BASE_IMAGE_TAR 2>&1
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha no docker load."; exit 1 }
    $loadOutput | ForEach-Object { Write-Host $_ }

    $exists = docker images --format "{{.Repository}}:{{.Tag}}" | Where-Object { $_ -eq $BASE_IMAGE }
    if ($exists) { return }

    $loadedImage = $loadOutput |
        Select-String "^Loaded image: " |
        ForEach-Object { $_.Line -replace "^Loaded image: ", "" } |
        Select-Object -First 1

    if (-not $loadedImage) {
        Write-Host "ERRO: nao foi possivel identificar a imagem carregada para retag."
        exit 1
    }

    docker tag $loadedImage $BASE_IMAGE
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha ao retaggear imagem base."; exit 1 }
}

if ($t -and $s) {
    foreach ($currentModule in $TARGET_MODULES) {
        $currentContainer = "test-${currentModule}-${PROFILE}"
        $exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $currentContainer }
        if ($exists) {
            docker stop $currentContainer | Out-Null
            docker rm $currentContainer | Out-Null
            Write-Host "Container removido: $currentContainer"
        }
    }
    exit 0
}

Ensure-BaseImage

foreach ($currentModule in $TARGET_MODULES) {
    $GRADLE_TASK = $GRADLE_TASKS[$currentModule]
    $DOCKERFILE = "docker/scripts/${currentModule}.Dockerfile"
    $MODULE_IMAGE = "finlumia/${currentModule}:${PROFILE}"
    $CONTAINER_PORT = $CONTAINER_PORTS[$currentModule]
    $HOST_PORT = $HOST_PORTS[$currentModule]
    $CONTAINER_TEST = "test-${currentModule}-${PROFILE}"

    if (-not (Test-Path $DOCKERFILE)) {
        Write-Host "ERRO: Dockerfile nao encontrado em $DOCKERFILE"
        exit 1
    }

    $devContainerRunning = docker ps --format "{{.Names}}" | Where-Object { $_ -eq $DEV_CONTAINER }
    if ($devContainerRunning) {
        $execUser = "root"
        docker exec $DEV_CONTAINER getent passwd finlumia *> $null
        if ($LASTEXITCODE -eq 0) { $execUser = "finlumia" }
        docker exec $DEV_CONTAINER bash -c @"
mkdir -p /workspace/.gradle
chown -R finlumia:finlumia /home/finlumia/.gradle /workspace/.gradle 2>/dev/null || true
find /workspace -maxdepth 3 -name 'build' -type d -exec chown -R finlumia:finlumia {} + 2>/dev/null || true
"@
        docker exec -u $execUser $DEV_CONTAINER bash -lc "./gradlew $GRADLE_TASK --no-daemon -Dspring.profiles.active=$PROFILE"
    } else {
        if (Test-Path ".\gradlew.bat") {
            .\gradlew.bat $GRADLE_TASK --no-daemon "-Dspring.profiles.active=$PROFILE"
        } else {
            .\gradlew $GRADLE_TASK --no-daemon "-Dspring.profiles.active=$PROFILE"
        }
    }
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha ao compilar $currentModule"; exit 1 }

    docker buildx build --load -t $MODULE_IMAGE -f $DOCKERFILE --build-arg SPRING_PROFILE=$PROFILE .
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha no build docker de $currentModule"; exit 1 }

    $OUTPUT_DIR = if ($t) { "docker/test" } else { "docker/build" }
    if (-not (Test-Path $OUTPUT_DIR)) { New-Item -ItemType Directory -Path $OUTPUT_DIR | Out-Null }
    $OUTPUT_FILE = "$OUTPUT_DIR/${currentModule}-${PROFILE}.tar"

    docker save -o $OUTPUT_FILE $MODULE_IMAGE
    if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha ao salvar imagem $MODULE_IMAGE"; exit 1 }

    if ($t) {
        $exists = docker ps -a --format "{{.Names}}" | Where-Object { $_ -eq $CONTAINER_TEST }
        if ($exists) {
            docker stop $CONTAINER_TEST | Out-Null
            docker rm $CONTAINER_TEST | Out-Null
        }

        $dockerRunArgs = @(
            "run", "-d",
            "--name", $CONTAINER_TEST,
            "-p", "${HOST_PORT}:${CONTAINER_PORT}",
            "-e", "SPRING_PROFILES_ACTIVE=$PROFILE",
            "--restart", "unless-stopped"
        )

        $dockerRunArgs += @(
            "-e", "SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:28079/finlumia_transactions"
        )

        if ($currentModule -eq "docs") {
            $dockerRunArgs += @(
                "-e", "DOCS_MODULES_BASE_URL_CONFIGURATOR=http://host.docker.internal:28081",
                "-e", "DOCS_MODULES_BASE_URL_IDENTIFY=http://host.docker.internal:28083",
                "-e", "DOCS_MODULES_BASE_URL_MOVEMENT=http://host.docker.internal:28084"
            )
        }

        $dockerRunArgs += $MODULE_IMAGE

        docker @dockerRunArgs
        if ($LASTEXITCODE -ne 0) { Write-Host "ERRO: falha ao iniciar container $CONTAINER_TEST"; exit 1 }
    }

    Write-Host "$currentModule | profile=$PROFILE | host=$HOST_PORT | container=$CONTAINER_PORT | image=$MODULE_IMAGE"
}

if ($t -and -not $all) {
    docker logs -f "test-${module}-${PROFILE}"
}
