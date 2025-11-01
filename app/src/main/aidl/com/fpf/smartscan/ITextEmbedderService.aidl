package com.fpf.smartscan;

import java.util.List;

interface ITextEmbedderService {
    int getEmbeddingDim();
    void closeSession();
    float[] embed(in String data);
    float[] embedBatch(in List<String> data);  // concatenate
}