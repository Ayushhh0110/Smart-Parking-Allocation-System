$libDir = "D:\NCU\SIH\2026-27\SmartParkingSystem\lib"
if (!(Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir -Force | Out-Null
}

$urls = @{
    "spring-core.jar"       = "https://repo1.maven.org/maven2/org/springframework/spring-core/6.1.14/spring-core-6.1.14.jar";
    "spring-beans.jar"      = "https://repo1.maven.org/maven2/org/springframework/spring-beans/6.1.14/spring-beans-6.1.14.jar";
    "spring-context.jar"    = "https://repo1.maven.org/maven2/org/springframework/spring-context/6.1.14/spring-context-6.1.14.jar";
    "spring-expression.jar" = "https://repo1.maven.org/maven2/org/springframework/spring-expression/6.1.14/spring-expression-6.1.14.jar";
    "spring-aop.jar"        = "https://repo1.maven.org/maven2/org/springframework/spring-aop/6.1.14/spring-aop-6.1.14.jar";
    "spring-jcl.jar"        = "https://repo1.maven.org/maven2/org/springframework/spring-jcl/6.1.14/spring-jcl-6.1.14.jar";
    "h2.jar"                = "https://repo1.maven.org/maven2/com/h2database/h2/2.3.232/h2-2.3.232.jar"
}

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls13

foreach ($name in $urls.Keys) {
    $dest = Join-Path $libDir $name
    if (!(Test-Path $dest)) {
        Write-Host "Downloading $name..."
        Invoke-WebRequest -Uri $urls[$name] -OutFile $dest -UseBasicParsing
        Write-Host "Saved $name ($((Get-Item $dest).Length) bytes)"
    } else {
        Write-Host "$name already exists."
    }
}
Write-Host "All libraries ready in $libDir"
