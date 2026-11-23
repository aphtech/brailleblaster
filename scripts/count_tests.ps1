# Prints the test-health counts tracked by test_fix.md. Run from anywhere:
#   pwsh scripts/count_tests.ps1
# Put the output in each test PR description; enabled=false, // @Test and
# BROKEN/TODO counts should only go down.
$root = Split-Path -Parent $PSScriptRoot
$sources = Get-ChildItem -Path $root -Directory |
    ForEach-Object { Join-Path $_.FullName 'src/test' } |
    Where-Object { Test-Path $_ } |
    ForEach-Object { Get-ChildItem -Path $_ -Recurse -File -Include *.java, *.kt }

function Measure-Matches([string]$pattern) {
    ($sources | Select-String -Pattern $pattern | Measure-Object).Count
}

function Measure-Files([string]$pattern) {
    ($sources | Select-String -Pattern $pattern -List | Measure-Object).Count
}

[ordered]@{
    'enabled = false markers'           = Measure-Matches 'enabled\s*=\s*false'
    '// @Test (commented out)'          = Measure-Matches '^\s*//\s*@Test'
    'files using BROKEN_TESTS/TODO_FIX_LATER' = Measure-Files 'BROKEN_TESTS|TODO_FIX_LATER'
    '@GuiTest classes'                  = Measure-Matches '^@GuiTest\b'
} | Format-Table -AutoSize -HideTableHeaders
