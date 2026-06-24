$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = $utf8
[Console]::OutputEncoding = $utf8

$projectRoot = Split-Path -Parent $PSScriptRoot
$composeArgs = @(
    'compose', '--env-file', (Join-Path $projectRoot '.env.local'),
    '-f', (Join-Path $projectRoot 'docker-compose.yml'),
    '-f', (Join-Path $projectRoot 'docker-compose.local.yml')
)
$mysqlClientArgs = @(
    'exec', '-T', 'mysql', 'mysql',
    '--default-character-set=utf8mb4',
    '-ucsuft_oj', '-plocal-csuft-oj-db-password'
)
$mysqlArgs = $composeArgs + $mysqlClientArgs + @('csuft_oj')
$mysqlQueryArgs = $composeArgs + $mysqlClientArgs + @('-N', 'csuft_oj')

$sqlPath = Join-Path $PSScriptRoot 'seed-signin-problems.sql'
$seedSql = Get-Content -LiteralPath $sqlPath -Raw -Encoding utf8
$containerSqlPath = '/tmp/seed-signin-problems.sql'
& docker @composeArgs 'cp' $sqlPath "mysql:$containerSqlPath"
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to copy seed SQL into MySQL container.'
}
"source $containerSqlPath;" | & docker @mysqlArgs
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to create sign-in problems.'
}

$titleMatches = [regex]::Matches($seedSql, "SELECT '([^']+)',")
$titles = @($titleMatches | ForEach-Object { $_.Groups[1].Value })
if ($titles.Count -ne 10) {
    throw "Expected 10 titles in seed SQL, found $($titles.Count)."
}

$casesByIndex = @(
    @(@('1 2', '3'), @('-5 8', '3'), @('100000 200000', '300000')),
    @(@('10 3', '7'), @('3 10', '-7'), @('-5 -8', '3')),
    @(@('6 7', '42'), @('-3 9', '-27'), @('0 99999', '0')),
    @(@('8 5', '8'), @('-2 -7', '-2'), @('42 42', '42')),
    @(@('12', 'EVEN'), @('7', 'ODD'), @('-4', 'EVEN')),
    @(@('20', '68'), @('0', '32'), @('-40', '-40')),
    @(@('Hello', 'HELLO'), @('csuft', 'CSUFT'), @('AlreadyUPPER', 'ALREADYUPPER')),
    @(@('csuft', 'tfusc'), @('a', 'a'), @('123abc', 'cba321')),
    @(@("5`n1 2 3 4 5", '15'), @("3`n-1 -2 10", '7'), @("1`n999999", '999999')),
    @(@('Education', '5'), @('sky', '0'), @('AEIOUaeiou', '10'))
)

function ConvertTo-SqlLiteral([string] $value) {
    if ($null -eq $value) { return 'NULL' }
    return "'" + $value.Replace('\', '\\').Replace("'", "''").Replace("`r", '').Replace("`n", '\n') + "'"
}

$titleHex = @{}
foreach ($title in $titles) {
    $hex = -join ($utf8.GetBytes($title) | ForEach-Object { $_.ToString('X2') })
    $titleHex[$hex] = $title
}
$hexList = ($titleHex.Keys | ForEach-Object { "'$_'" }) -join ','
$query = "SELECT id,HEX(title) FROM tb_problem WHERE HEX(title) IN ($hexList) ORDER BY id;"
$rows = $query | & docker @mysqlQueryArgs
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to read created problem IDs.'
}

$ids = @{}
foreach ($row in $rows) {
    $parts = $row -split "`t", 2
    if ($parts.Count -eq 2) {
        $hex = $parts[1].ToUpperInvariant()
        if ($titleHex.ContainsKey($hex)) {
            $ids[$titleHex[$hex]] = [long] $parts[0]
        }
    }
}
if ($ids.Count -ne $titles.Count) {
    throw "Expected $($titles.Count) problems, found $($ids.Count)."
}

$dataRoot = 'C:\csuft-oj-data\testcases'
$testCaseSql = New-Object System.Collections.Generic.List[string]
$testCaseSql.Add('SET NAMES utf8mb4;')

for ($titleIndex = 0; $titleIndex -lt $titles.Count; $titleIndex++) {
    $title = $titles[$titleIndex]
    $problemId = $ids[$title]
    $problemDir = Join-Path $dataRoot $problemId
    New-Item -ItemType Directory -Force $problemDir | Out-Null
    Get-ChildItem -LiteralPath $problemDir -File -ErrorAction SilentlyContinue | Remove-Item -Force
    $testCaseSql.Add("DELETE FROM tb_test_case WHERE problem_id=$problemId;")

    $order = 1
    foreach ($case in $casesByIndex[$titleIndex]) {
        $inputName = "$order.in"
        $outputName = "$order.out"
        $inputText = [string] $case[0]
        $outputText = [string] $case[1]
        [System.IO.File]::WriteAllText((Join-Path $problemDir $inputName), $inputText + [Environment]::NewLine, $utf8)
        [System.IO.File]::WriteAllText((Join-Path $problemDir $outputName), $outputText + [Environment]::NewLine, $utf8)
        $score = if ($order -eq 1) { 34 } else { 33 }
        $testCaseSql.Add(
            "INSERT INTO tb_test_case (problem_id,input_path,output_path,input_preview,output_preview,score,sort_order,created_at) VALUES (" +
            "$problemId," + (ConvertTo-SqlLiteral $inputName) + ',' + (ConvertTo-SqlLiteral $outputName) + ',' +
            (ConvertTo-SqlLiteral $inputText) + ',' + (ConvertTo-SqlLiteral $outputText) + ",$score,$order,NOW());"
        )
        $order++
    }
}

($testCaseSql -join [Environment]::NewLine) | & docker @mysqlArgs
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to register sign-in problem test cases.'
}

Write-Host "Created or refreshed $($titles.Count) sign-in problems with 3 test cases each."
foreach ($title in $titles) {
    Write-Host ("#{0} {1}" -f $ids[$title], $title)
}
