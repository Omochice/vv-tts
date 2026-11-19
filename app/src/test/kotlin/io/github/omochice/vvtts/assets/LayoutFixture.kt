package io.github.omochice.vvtts.assets

import java.io.File

internal fun File.create(text: String = "copied by hand") = apply {
  parentFile?.mkdirs()
  writeText(text)
}

internal fun AssetLayout.installOnnxRuntimeByHand() = onnxRuntimeLibrary.create()

internal fun AssetLayout.installDictionaryByHand() = File(dictionaryDir, "sys.dic").create()

internal fun AssetLayout.installBasePartsByHand() {
  installOnnxRuntimeByHand()
  installDictionaryByHand()
}
