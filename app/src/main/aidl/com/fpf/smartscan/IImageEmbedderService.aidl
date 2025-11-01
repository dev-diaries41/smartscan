// IImageEmbedderService.aidl
package com.fpf.smartscan;

import java.util.List;

interface IImageEmbedderService {
    int getEmbeddingDim();
    byte[] getDelimiter();
    void closeSession();
    float[] embed(in byte[] data);
    float[] embedBatch(in byte[] data); //inputs and outputs are concatenated
}