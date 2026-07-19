[CmdletBinding()]
param(
    [string]$CodexCommand = "codex",
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$CodexArguments = @()
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

if ($null -eq (Get-Command $CodexCommand -ErrorAction SilentlyContinue)) {
    throw "Comando Codex não encontrado: '$CodexCommand'."
}

$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$secureToken = Read-Host -Prompt "Cole o token do SonarQube para esta sessão" -AsSecureString
$tokenPointer = [IntPtr]::Zero
try {
    $tokenPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
    $plainToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPointer)
    if ([string]::IsNullOrWhiteSpace($plainToken)) {
        throw "O token não pode ser vazio."
    }
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $plainToken, "Process")
    $plainToken = $null

    & $CodexCommand @CodexArguments
    if ($LASTEXITCODE -ne 0) {
        throw "O Codex terminou com código de saída $LASTEXITCODE."
    }
}
finally {
    if ($tokenPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPointer)
    }
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $previousToken, "Process")
    Remove-Variable secureToken -ErrorAction SilentlyContinue
    Remove-Variable plainToken -ErrorAction SilentlyContinue
}
