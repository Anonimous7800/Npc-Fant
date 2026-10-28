$src  = "src\main\java\com\example\advancednpc"
$dst  = "NpcfantCore\src\main\java\com\example\advancednpc"
$rsrc = "src\main\resources"
$rdst = "NpcfantCore\src\main\resources"

if (Test-Path $src) {
    Get-ChildItem -Path $src -Recurse -Filter "*.java" | ForEach-Object {
        $file = $_
        $rel  = $file.FullName.Substring((Resolve-Path $src).Path.Length + 1)
        $dest = Join-Path $dst $rel
        New-Item -ItemType Directory -Force -Path (Split-Path $dest) | Out-Null
        Copy-Item $file.FullName -Destination $dest -Force
        Write-Host "Moved: $rel"
    }
}

if (Test-Path $rsrc) {
    Get-ChildItem -Path $rsrc -File | ForEach-Object {
        $file = $_
        Copy-Item $file.FullName -Destination $rdst -Force
        Write-Host "Moved resource: $($file.Name)"
    }
}

Write-Host "Migracion completada."
