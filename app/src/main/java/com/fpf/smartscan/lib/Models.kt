package com.fpf.smartscan.lib

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.constants.modelPathsMap
import com.fpf.smartscan.data.DownloadableModel
import com.fpf.smartscan.data.ImportedModel
import com.fpf.smartscan.data.SmartScanModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

fun getDownloadableModels(context: Context): List<DownloadableModel>{
    val facialRecognitionModel = DownloadableModel(
        type = SmartScanModelType.FACE,
        name = context.getString(R.string.facial_recognition_model_name),
        url = context.getString(R.string.inception_resnet_v1_model_url),
    )
    return listOf(facialRecognitionModel)
}

fun getImportedModels(context: Context): List<ImportedModel>{
    try {
        val facialRecognitionModel = ImportedModel(
            type = SmartScanModelType.FACE,
            name = context.getString(R.string.facial_recognition_model_name),
            dependentModelPaths = modelPathsMap[SmartScanModelType.FACE]!!.dependentModelPaths,
        )
        val importedModels = mutableListOf(facialRecognitionModel).filter {  isImported(context, it)}
        return importedModels
    }catch (e: Exception){
        Log.e("getImportedModels", "Error getting imported models: ${e.message}")
        return emptyList()
    }
}

private fun isImported(context: Context, model: ImportedModel): Boolean{
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

suspend fun importModel(context: Context, uri: Uri, type: SmartScanModelType) = withContext(Dispatchers.IO){
    val modelInfo = modelPathsMap[type] ?: return@withContext
    val outputPath = modelInfo.path
    val outputFile = File(context.filesDir, outputPath)
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