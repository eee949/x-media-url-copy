param([Parameter(Mandatory=$true)][string]$AndroidJar,[string]$Jdk=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$Jdk){$Jdk=Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source)}
$root=$PSScriptRoot;$output=Join-Path $root ('build/test-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force $output | Out-Null
$sources=Get-ChildItem "$root/src" -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& "$Jdk/bin/javac.exe" -encoding UTF-8 -cp $AndroidJar -d $output @sources "$root/tests/NativeMediaTest.java"
if($LASTEXITCODE){throw 'Test compilation failed'}
& "$Jdk/bin/java.exe" -cp "$output;$AndroidJar" NativeMediaTest
if($LASTEXITCODE){throw 'Tests failed'}
