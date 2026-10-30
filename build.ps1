param(
 [Parameter(Mandatory=$true)][string]$Runtime,
 [Parameter(Mandatory=$true)][string]$AndroidJar,
 [Parameter(Mandatory=$true)][string]$R8,
 [Parameter(Mandatory=$true)][string]$KotlinCompiler,
 [Parameter(Mandatory=$true)][string]$Annotations,
 [string]$Jdk=$env:JAVA_HOME,
 [string]$Version='0.1.0'
)
$ErrorActionPreference='Stop'
if(!$Jdk){$Jdk=Split-Path (Split-Path (Get-Command javac.exe -ErrorAction Stop).Source)}
foreach($inputFile in @($Runtime,$AndroidJar,$R8,$KotlinCompiler,$Annotations)){if(!(Test-Path -LiteralPath $inputFile -PathType Leaf)){throw "Missing build input: $inputFile"}}
if($Version -notmatch '^\d+\.\d+\.\d+([-.][A-Za-z0-9.-]+)?$'){throw 'Invalid version'}
$root=$PSScriptRoot
$build=Join-Path $root ('build/'+[Guid]::NewGuid().ToString('N'))
$dist=Join-Path $root 'dist'
New-Item -ItemType Directory -Force "$build/ext","$build/patch","$build/extdex","$build/patchdex",$dist | Out-Null
$javaSources=Get-ChildItem "$root/src" -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& "$Jdk/bin/javac.exe" -encoding UTF-8 --release 17 -cp $AndroidJar -d "$build/ext" @javaSources
if($LASTEXITCODE){throw 'Extension compilation failed'}
& "$Jdk/bin/java.exe" -cp "$KotlinCompiler;$Annotations;$Runtime" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -classpath $Runtime -jvm-target 17 -d "$build/patch" "$root/MediaUrlCopyPatch.kt"
if($LASTEXITCODE){throw 'Patch compilation failed'}
& "$Jdk/bin/jar.exe" "-J-Djava.io.tmpdir=$build" cf "$build/ext.jar" -C "$build/ext" local
if($LASTEXITCODE){throw 'Extension packaging failed'}
& "$Jdk/bin/jar.exe" "-J-Djava.io.tmpdir=$build" cf "$build/patch.jar" -C "$build/patch" .
if($LASTEXITCODE){throw 'Patch packaging failed'}
& "$Jdk/bin/java.exe" -cp $R8 com.android.tools.r8.D8 --min-api 26 --lib $AndroidJar --output "$build/extdex" "$build/ext.jar"
if($LASTEXITCODE){throw 'Extension DEX failed'}
& "$Jdk/bin/java.exe" -cp $R8 com.android.tools.r8.D8 --min-api 26 --classpath $Runtime --output "$build/patchdex" "$build/patch.jar"
if($LASTEXITCODE){throw 'Patch DEX failed'}
$output=Join-Path $dist "media-url-copy-$Version.mpp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=Join-Path $build 'bundle.zip'
$zip=[IO.Compression.ZipFile]::Open($archive,[IO.Compression.ZipArchiveMode]::Create)
try{
 $classes=(Resolve-Path "$build/patch").Path
 foreach($file in Get-ChildItem $classes -Recurse -File){$rel=$file.FullName.Substring($classes.Length+1).Replace('\','/');[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,$rel)|Out-Null}
 foreach($file in Get-ChildItem "$build/patchdex" -Filter '*.dex'){[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,$file.Name)|Out-Null}
 [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build/extdex/classes.dex",'extensions/media-url-copy.dex')|Out-Null
 foreach($name in @('LICENSE','NOTICE')){[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $root $name),$name)|Out-Null}
 $entry=$zip.CreateEntry('META-INF/MANIFEST.MF');$writer=[IO.StreamWriter]::new($entry.Open(),[Text.UTF8Encoding]::new($false))
 $writer.Write("Manifest-Version: 1.0`r`nName: Media URL Copy`r`nDescription: Standalone X media URL clipboard patch`r`nVersion: $Version`r`nAuthor: Media URL Copy contributors`r`nLicense: GNU General Public License v3.0`r`nPatcher-Version: 2.0.1`r`n`r`n");$writer.Dispose()
}finally{$zip.Dispose()}
Copy-Item -LiteralPath $archive -Destination $output -Force
Write-Output $output
