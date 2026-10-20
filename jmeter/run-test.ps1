# ============================================================
# FlashSale - One-Click Load Test Script
# Usage: Double-click run-test.bat
# ============================================================
$ErrorActionPreference = "Stop"

# ---- Config ----
$JMETER_BIN    = "D:\jmeter\apache-jmeter-5.6.3\bin"
$SCRIPT_DIR    = Split-Path -Parent $MyInvocation.MyCommand.Path
$JMX           = Join-Path $SCRIPT_DIR "flashsale-load-test.jmx"
$RESULT_DIR    = Join-Path $SCRIPT_DIR "results"
$RESULT_JTL    = Join-Path $RESULT_DIR "result.jtl"
$REPORT_DIR    = Join-Path $RESULT_DIR "report"
$MYSQL_USER    = "root"
$MYSQL_PASS    = $env:MYSQL_PASSWORD   # 从环境变量读取，勿写入仓库

$MYSQL_DB      = "flashsale"
$BACKEND       = "http://localhost:8080"

Clear-Host

Write-Host ""
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "    FlashSale - One-Click Load Test Tool      " -ForegroundColor Cyan
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""

if (-not $MYSQL_PASS) {
    Write-Host "  [FAIL] Environment variable MYSQL_PASSWORD is not set" -ForegroundColor Red
    Write-Host "         PowerShell: `$env:MYSQL_PASSWORD = 'your-password'" -ForegroundColor Red
    pause; exit 1
}

# ============================================================
# [1/5] Check Environment
# ============================================================
Write-Host "  [1/5] Checking environment..." -ForegroundColor Yellow
Write-Host "  ----------------------------------------"

# JMeter
if (Test-Path "$JMETER_BIN\jmeter.bat") {
    Write-Host "  [OK] JMeter: $JMETER_BIN" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] JMeter not found: $JMETER_BIN" -ForegroundColor Red
    Write-Host "         Please extract JMeter to D:\jmeter\" -ForegroundColor Red
    pause; exit 1
}

# Test Plan
if (Test-Path $JMX) {
    Write-Host "  [OK] Test Plan: flashsale-load-test.jmx" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] Test Plan not found: $JMX" -ForegroundColor Red
    pause; exit 1
}

# Backend
try {
    $null = Invoke-WebRequest -Uri "$BACKEND/api/goods" -TimeoutSec 3 -UseBasicParsing
    Write-Host "  [OK] Backend: $BACKEND" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] Backend not responding, run: mvn spring-boot:run" -ForegroundColor Yellow
}

# Redis
$redisOk = $false
try {
    $r = redis-cli PING 2>&1
    if ($r -match "PONG") {
        Write-Host "  [OK] Redis: localhost:6379" -ForegroundColor Green
        $redisOk = $true
    } else {
        Write-Host "  [WARN] Redis response unexpected" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [WARN] Redis unreachable" -ForegroundColor Yellow
}

# MySQL
$mysqlOk = $false
try {
    $m = mysql -u$MYSQL_USER -p$MYSQL_PASS -e "SELECT 1" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] MySQL: localhost:3306" -ForegroundColor Green
        $mysqlOk = $true
    } else {
        Write-Host "  [WARN] MySQL unreachable" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [WARN] MySQL unreachable" -ForegroundColor Yellow
}

# RabbitMQ
try {
    $cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("guest:guest"))
    $null = Invoke-WebRequest -Uri "http://localhost:15672/api/overview" -Headers @{Authorization="Basic $cred"} -TimeoutSec 3 -UseBasicParsing
    Write-Host "  [OK] RabbitMQ: localhost:5672" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] RabbitMQ unreachable, seckill will fail" -ForegroundColor Yellow
}

Write-Host ""

# ============================================================
# [2/5] Choose Load Level
# ============================================================
Write-Host "  [2/5] Choose load level" -ForegroundColor Yellow
Write-Host "  ----------------------------------------"
Write-Host "    [1] Light   -  50 users /  30 sec"
Write-Host "    [2] Medium  - 200 users /  60 sec  (recommended)"
Write-Host "    [3] Heavy   - 500 users / 120 sec"
Write-Host "    [C] Custom"
Write-Host "  ----------------------------------------"

$choice = Read-Host "   Enter option [1/2/3/C] (default=2)"
if ($choice -eq "") { $choice = "2" }

switch ($choice) {
    "1" {
        $THREADS  = 50
        $RAMPUP   = 5
        $DURATION = 30
        $LABEL    = "Light (50 users / 30 sec)"
    }
    "2" {
        $THREADS  = 200
        $RAMPUP   = 10
        $DURATION = 60
        $LABEL    = "Medium (200 users / 60 sec)"
    }
    "3" {
        $THREADS  = 500
        $RAMPUP   = 10
        $DURATION = 120
        $LABEL    = "Heavy (500 users / 120 sec)"
    }
    "C" {
        $THREADS  = if (($x = Read-Host "   Threads (default=200)")) { $x } else { 200 }
        $RAMPUP   = if (($x = Read-Host "   Ramp-up sec (default=10)")) { $x } else { 10 }
        $DURATION = if (($x = Read-Host "   Duration sec (default=60)")) { $x } else { 60 }
        $LABEL    = "Custom (${THREADS} users / ${DURATION} sec)"
    }
    default {
        $THREADS  = 200
        $RAMPUP   = 10
        $DURATION = 60
        $LABEL    = "Medium (200 users / 60 sec)"
    }
}

$ST = if ($THREADS -gt 500) { 500 } else { $THREADS }

Write-Host ""
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "    $LABEL"                                     -ForegroundColor Cyan
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================
# [3/5] Clean Old Results
# ============================================================
Write-Host "  [3/5] Cleaning old results..." -ForegroundColor Yellow
if (Test-Path $RESULT_DIR) {
    Remove-Item -Recurse -Force $RESULT_DIR -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Path $RESULT_DIR -Force | Out-Null
Write-Host "  [OK] Results directory cleaned" -ForegroundColor Green

# ============================================================
# [4/5] Run JMeter
# ============================================================
Write-Host ""
Write-Host "  [4/5] Running load test..." -ForegroundColor Yellow
Write-Host "  ----------------------------------------"
Write-Host "   Press Ctrl+C to abort" -ForegroundColor DarkGray
Write-Host "  ----------------------------------------"
Write-Host ""

# Replace placeholders in JMX with actual values
$JMX_TEMP = Join-Path $RESULT_DIR "flashsale-temp.jmx"
$USERS_CSV = Join-Path $SCRIPT_DIR "users.csv"
$jmxContent = Get-Content $JMX -Raw -Encoding UTF8
$jmxContent = $jmxContent -replace "@@THREADS@@", $THREADS
$jmxContent = $jmxContent -replace "@@RAMPUP@@", $RAMPUP
$jmxContent = $jmxContent -replace "@@DURATION@@", $DURATION
$jmxContent = $jmxContent -replace "@@SETUP_THREADS@@", $ST
$jmxContent = $jmxContent -replace "@@SETUP_RAMPUP@@", "5"
# Fix CSV path to absolute (temp JMX is in results/ subdir)
$jmxContent = $jmxContent -replace ">users.csv<", ">$USERS_CSV<"
# Write without BOM to avoid XML parser errors
[System.IO.File]::WriteAllText($JMX_TEMP, $jmxContent, [System.Text.UTF8Encoding]::new($false))

Write-Host "  [OK] JMX prepared: threads=$THREADS, rampup=$RAMPUP, duration=$DURATION" -ForegroundColor Green

$jmeterArgs = @(
    "-n",
    "-t", $JMX_TEMP,
    "-l", $RESULT_JTL,
    "-e", "-o", $REPORT_DIR
)

& "$JMETER_BIN\jmeter.bat" @jmeterArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "  [FAIL] JMeter exited with code $LASTEXITCODE" -ForegroundColor Red
    pause; exit 1
}

Write-Host ""
Write-Host "  [OK] Load test completed!" -ForegroundColor Green

# ============================================================
# [5/5] Verify Data
# ============================================================
Write-Host ""
Write-Host "  [5/5] Verifying data correctness..." -ForegroundColor Yellow
Write-Host "  ----------------------------------------"

if ($mysqlOk) {
    # Check oversell
    Write-Host ""
    Write-Host "  --- Order count vs Stock ---" -ForegroundColor DarkGray
    $sql1 = "SELECT CONCAT('  GoodsID=', g.id, '  Stock=', g.stock_count, '  Orders=', IFNULL(COUNT(o.id),0), '  ', CASE WHEN g.stock_count >= IFNULL(COUNT(o.id),0) THEN '[OK] No oversell' ELSE '[FAIL] OVERSELL!' END) FROM goods g LEFT JOIN order_info o ON o.goods_id = g.id WHERE g.id = 1 GROUP BY g.id;"
    mysql -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -N -e $sql1 2>$null

    # Check duplicate
    Write-Host ""
    Write-Host "  --- Duplicate check (should be empty) ---" -ForegroundColor DarkGray
    $sql2 = "SELECT COUNT(*) FROM (SELECT user_id, goods_id FROM seckill_order GROUP BY user_id, goods_id HAVING COUNT(*) > 1) t;"
    $dup = mysql -u$MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -N -e $sql2 2>$null
    if ($dup -eq "0") {
        Write-Host "  [OK] No duplicate orders, dedup works" -ForegroundColor Green
    } elseif ($dup) {
        Write-Host "  [FAIL] Found $dup duplicate orders!" -ForegroundColor Red
    } else {
        Write-Host "  [SKIP] Query failed" -ForegroundColor DarkGray
    }

    # Redis stock
    Write-Host ""
    Write-Host "  --- Redis stock after test ---" -ForegroundColor DarkGray
    $redisStock = redis-cli GET seckill:stock:1 2>$null
    Write-Host "   Goods 1 Redis stock: $redisStock"
} else {
    Write-Host "  [SKIP] MySQL unreachable" -ForegroundColor DarkGray
}

# ============================================================
# Done
# ============================================================
Write-Host ""
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "             LOAD TEST COMPLETE               " -ForegroundColor Cyan
Write-Host "  ============================================" -ForegroundColor Cyan

if (Test-Path $RESULT_JTL) {
    $lines = (Get-Content $RESULT_JTL | Where-Object { $_ -notmatch "^timeStamp" }).Count
    Write-Host "    Total requests: $lines" -ForegroundColor White
}

Write-Host ""
Write-Host "    HTML Report: results\report\index.html" -ForegroundColor White
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""

$reportPath = Join-Path $REPORT_DIR "index.html"
if (Test-Path $reportPath) {
    Start-Process $reportPath
    Write-Host "  Report opened in browser" -ForegroundColor Green
} else {
    Write-Host "  Open manually: $reportPath" -ForegroundColor Yellow
}

Write-Host ""
pause
