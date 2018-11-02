/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.labs;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.labs.api.SpeechToTextResponse;

import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.WordInfo;

public class SpeechToTextGoogleResponse implements SpeechToTextResponse {

    RecognizeResponse response = null;

    public SpeechToTextGoogleResponse(RecognizeResponse response) {
        this.response = response;
    }
    
    /*
     * We get the first resiult and the first alternative
     */
    protected SpeechRecognitionAlternative getFirstAlternative() {
        
        List<SpeechRecognitionResult> results = response.getResultsList();
        
        SpeechRecognitionResult result = results.get(0);
        
        // There can be several alternative transcripts for a given chunk of speech.
        // Just use the first (most likely) one here.
        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
        
        return alternative;
    }

    /*
     * In this implementation, we get the first result and its first alternative.
     */
    @Override
    public String getText() {

        SpeechRecognitionAlternative alternative = getFirstAlternative();

        return alternative.getTranscript();
    }
    
    @Override
    public JSONArray getWordTimeOffsets() throws JSONException {
        
        SpeechRecognitionAlternative alternative = getFirstAlternative();
        
        JSONArray array = new JSONArray();
        
        for (WordInfo wordInfo : alternative.getWordsList()) {
            
            JSONObject obj = new JSONObject();
            
            obj.put("word", wordInfo.getWord());
            obj.put("start", wordInfo.getStartTime().getSeconds());
            obj.put("end", wordInfo.getEndTime().getSeconds());
            
            array.put(obj);
        }
        
        
        return array;
    }

    @Override
    public Object getNativeResponse() {
        return response;
    }

}
