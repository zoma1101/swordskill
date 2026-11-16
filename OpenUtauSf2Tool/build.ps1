param([switch]$Install)

$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$src = [IO.Path]::Combine($root, 'src')
$distRoot = [IO.Path]::Combine($root, 'dist')
$dist = [IO.Path]::Combine($distRoot, 'Sf2 SoundFont Tool')
$exe = [IO.Path]::Combine($dist, 'OpenUtauSf2Tool.exe')

[IO.Directory]::CreateDirectory($dist) | Out-Null

$csc = [IO.Path]::Combine($env:WINDIR, 'Microsoft.NET\Framework64\v4.0.30319\csc.exe')
if (-not [IO.File]::Exists($csc)) {
    $csc = [IO.Path]::Combine($env:WINDIR, 'Microsoft.NET\Framework\v4.0.30319\csc.exe')
}
if (-not [IO.File]::Exists($csc)) {
    throw 'C# コンパイラ (csc.exe) が見つかりません。.NET Framework 4.x が必要です。'
}

$files = [IO.Directory]::GetFiles($src, '*.cs')
$cscArgs = @(
    '/nologo',
    '/target:winexe',
    ('/out:' + $exe),
    '/optimize+',
    '/reference:System.dll',
    '/reference:System.Core.dll',
    '/reference:System.Windows.Forms.dll',
    '/reference:System.Drawing.dll'
) + $files

& $csc $cscArgs
if ($LASTEXITCODE -ne 0) {
    throw ('コンパイルに失敗しました (exit code ' + $LASTEXITCODE + ')')
}

Copy-Item -LiteralPath ([IO.Path]::Combine($root, 'plugin', 'plugin.txt')) -Destination ([IO.Path]::Combine($dist, 'plugin.txt')) -Force
Copy-Item -LiteralPath ([IO.Path]::Combine($root, 'README.md')) -Destination ([IO.Path]::Combine($dist, 'README.md')) -Force

Write-Host ''
Write-Host ('ビルド完了: ' + $exe)

if ($Install) {
    $plugins = [IO.Path]::Combine([Environment]::GetFolderPath('MyDocuments'), 'OpenUtau', 'Plugins')
    $target = [IO.Path]::Combine($plugins, 'Sf2 SoundFont Tool')
    [IO.Directory]::CreateDirectory($target) | Out-Null
    foreach ($file in [IO.Directory]::GetFiles($dist)) {
        [IO.File]::Copy($file, [IO.Path]::Combine($target, [IO.Path]::GetFileName($file)), $true)
    }
    foreach ($dir in [IO.Directory]::GetDirectories($dist)) {
        $sub = [IO.Path]::Combine($target, [IO.Path]::GetFileName($dir))
        [IO.Directory]::CreateDirectory($sub) | Out-Null
        foreach ($file in [IO.Directory]::GetFiles($dir)) {
            [IO.File]::Copy($file, [IO.Path]::Combine($sub, [IO.Path]::GetFileName($file)), $true)
        }
    }
    Write-Host ('インストール先: ' + $target)
    Write-Host 'OpenUtau を再起動してください。'
}
