package com.fpf.smartscan.lib

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.modelPathsMap
import com.fpf.smartscan.data.ModelInfo
import com.fpf.smartscan.data.ImportedModel
import com.fpf.smartscan.data.SmartScanModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

fun getDownloadableModels(context: Context): List<ModelInfo>{
    val facialRecognitionModel = ModelInfo(
        type = SmartScanModelType.FACE,
        name = context.getString(R.string.inception_resnet_v1_model),
        url = context.getString(R.string.inception_resnet_v1_model_url),
    )
    return listOf(facialRecognitionModel)
}

fun getImportedModels(context: Context): List<ImportedModel>{
    try {
        val facialRecognitionModel = ImportedModel(
            type = SmartScanModelType.FACE,
            name = context.getString(R.string.inception_resnet_v1_model),
            dependentModelPaths = modelPathsMap[SmartScanModelType.FACE]!!.dependentModelPaths,
        )
        val importedModels = mutableListOf(facialRecognitionModel).filter {  isImported(context, it)}
        return importedModels
    }catch (e: Exception){
        Log.e("getImportedModels", "Error getting imported models: ${e.message}")
        return emptyList()
    }
}

fun isImported(context: Context, model: ImportedModel): Boolean{
    if(File(context.filesDir, modelPathsMap[model.type]!!.path).exists()) {
        return true
    }else if(model.dependentModelPaths.isNotEmpty()){
        return model.dependentModelPaths.any{File(context.filesDir, it).exists()}
    }
    return false
}

fun deleteModel(context: Context, model: ImportedModel): Boolean{
    try {
        val modelPathInfo = modelPathsMap[model.type]!!
        val file = File(context.filesDir, modelPathInfo.path )
        file.delete()

        if(modelPathInfo.dependentModelPaths.isNotEmpty()){
            modelPathInfo.dependentModelPaths.forEach {File(context.filesDir, it ).delete()  }
        }
        return true
    }catch (e: Exception){
        Log.e("deleteModel", "Error deleting model: ${e.message}")
        return false
    }
}

fun getTypeFromUri(context: Context, uri: Uri): SmartScanModelType?{
    val models = getDownloadableModels(context)
    val modelFilenames = models.map { it.url.split("/").last() }
    val importedFilename = getFileName(context, uri)
    val index = modelFilenames.indexOfFirst{ it == importedFilename}
    if(index < 0) return null

    return models[index].type
}

suspend fun importModel(context: Context, uri: Uri) = withContext(Dispatchers.IO){
    val type = getTypeFromUri(context, uri)
    val modelInfo = modelPathsMap[type] ?: error("Invalid model file")
    val outputPath = modelInfo.path
    val outputFile = File(context.filesDir, outputPath)

    outputFile.parentFile?.mkdirs()

    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(outputFile).use { output -> input.copyTo(output) }
    }

    // If it's a zip, unzip to the same folder
    if (outputFile.extension == "zip") {
        val targetDir = File(outputFile.parentFile, outputFile.nameWithoutExtension)
        if (!targetDir.exists()) targetDir.mkdirs()

        ZipInputStream(FileInputStream(outputFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryFile = File(targetDir, entry.name)
                FileOutputStream(entryFile).use { out ->
                    zip.copyTo(out)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }
    outputFile.delete()
}