# UltrawideUnlocker

Modul LSPosed untuk membuka lensa ultrawide (0.5x / 0.6x) pada aplikasi kamera yang dipilih.

## Fitur
- Mengubah `CONTROL_ZOOM_RATIO_RANGE` (zoom minimum 0.5x)
- Menampilkan Camera ID tersembunyi (2, 3, 4) bila memang bisa diakses
- Melewati whitelist paket aux kamera vendor (`camera.aux.packagelist`)
- Build APK otomatis lewat GitHub Actions

## Cara pakai
1. Unduh APK dari tab Actions -> run terbaru -> Artifacts.
2. Pasang APK, aktifkan modul di LSPosed, lalu pilih aplikasi kamera pada Scope.
3. Force stop aplikasi kamera, lalu buka lagi.

## Catatan teknis
Proyek ini memakai modul `xposed-stub` (hanya untuk compile, tidak ikut masuk APK)
sehingga build tidak bergantung pada server `api.xposed.info`.
