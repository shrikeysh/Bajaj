$ErrorActionPreference = "Stop"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven was not found on PATH. Install Maven, then run this script again."
}

mvn clean package
