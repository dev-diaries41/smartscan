package com.fpf.smartscan.lib

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.constants.INCEPTION_RESNET_DEP_FACE_DETECTOR
import com.fpf.smartscan.constants.INCEPTION_RESNET_DEP_INCEPTION
import com.fpf.smartscan.constants.MODEL_DIR
import com.fpf.smartscan.constants.downloadableModels
import com.fpf.smartscan.constants.facialRecognitionModel
import com.fpf.smartscan.constants.miniLmTextEmbedderModel
import com.fpf.smartscan.data.ImportedModel
import com.fpf.smartscan.data.ModelPathInfo
import com.fpf.smartscan.data.SmartScanModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun filenameFromUrl(url:String) = url.split("/").last()

fun getModelPathMap(): Map<String, ModelPathInfo>{
    val inceptionResnetFilename = filenameFromUrl(facialRecognitionModel.url)
    val inceptionResnetDir = inceptionResnetFilename.substringBefore(".")

    val modelsPathMap = mapOf(
        facialRecognitionModel.name to ModelPathInfo(
            path = "$MODEL_DIR/$inceptionResnetFilename",
            dependentModelPaths = listOf(
                "$MODEL_DIR/$inceptionResnetDir/$INCEPTION_RESNET_DEP_FACE_DETECTOR",
                "$MODEL_DIR/$inceptionResnetDir/$INCEPTION_RESNET_DEP_INCEPTION"
            )
        ),
        miniLmTextEmbedderModel.name to ModelPathInfo("$MODEL_DIR/${filenameFromUrl(miniLmTextEmbedderModel.url)}"),
    )
    return modelsPathMap
}

fun getTypeFromName(name: String): SmartScanModelType{
    return downloadableModels.find{it.name == name}?.type?: error("error getting type")
}
fun getModelNameFromUri(context: Context, uri: Uri): String{
    val modelFilenames = downloadableModels.map { filenameFromUrl(it.url) }
    val importedFilename = getFileName(context, uri)
    val index = modelFilenames.indexOfFirst{ it == importedFilename}
    if(index < 0) error("error getting model name")

    return downloadableModels[index].name
}

fun getImportedModels(context: Context): List<ImportedModel>{
    val importedModels = mutableListOf<ImportedModel>()
    try {
        val modelPathsMap = getModelPathMap()
        for((name, pathInfo) in modelPathsMap.entries){
            if(pathInfo.dependentModelPaths.isNotEmpty()){
                val files = pathInfo.dependentModelPaths.map{File(context.filesDir, it)}
                if(files.all{it.exists()}){
                    importedModels.add(ImportedModel(
                        type = getTypeFromName(name),
                        name = name,
                        dependentModelPaths = pathInfo.dependentModelPaths
                    ))
                }
            }
            else if(File(context.filesDir, pathInfo.path).exists()) {
                importedModels.add(ImportedModel(
                    type = getTypeFromName(name),
                    name = name,
                ))
            }
        }
        return importedModels
    }catch (e: Exception){
        Log.e("getImportedModels", "Error getting imported models: ${e.message}")
        return importedModels
    }
}

fun deleteModel(context: Context, model: ImportedModel): Boolean{
    try {
        val modelPathsMap = getModelPathMap()
        val modelPathInfo = modelPathsMap[model.name]!!
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

suspend fun importModel(context: Context, uri: Uri) = withContext(Dispatchers.IO){
    val modelPathsMap = getModelPathMap()
    val modelName = getModelNameFromUri(context, uri)
    val modelInfo = modelPathsMap[modelName]!!
    val outputFile = File(context.filesDir, modelInfo.path)

    outputFile.parentFile?.mkdirs()

    copyFromUri(context, uri, outputFile)

    if (outputFile.extension == "zip") {
        val targetDir = File(outputFile.parentFile, outputFile.nameWithoutExtension)
        if (!targetDir.exists()) targetDir.mkdirs()

        val extractedFiles = unzipFiles(outputFile, targetDir)
        val extractedFilesPaths = extractedFiles.map{it.path}
        val isValid = extractedFilesPaths.all{ extractedPath ->
            modelInfo.dependentModelPaths.any{ dependency -> extractedPath.contains(dependency)}
        }

        try{
            if(!isValid){
                extractedFiles.forEach { it.delete() }
                error("Invalid model file")
            }
        }finally {
            outputFile.delete()
        }
    }
}