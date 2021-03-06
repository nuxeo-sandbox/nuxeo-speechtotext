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
package org.nuxeo.labs.speechtotext.google;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.labs.speechtotext.api.SpeechToTextResponse;

/**
 * Encapsulate the response received after a call to <code>recognize</code> via REST. <br>
 * <br>
 * See Google doc. at https://cloud.google.com/speech-to-text/docs/reference/rest/v1/speech/recognize. Basically, as of
 * API V1, the result is a JSON Object as string.
 * <p>
 * See <code>com.google.protobuf.Duration</code> for word infos. Basically,n the result is expressed in a string, milli
 * and nano seconds being the fractional part. In this implementation, we just keep the seconds with max 3 digits (like:
 * "5.050006765s" => 5.05)
 * 
 * <pre>
 *  {
 *    "results": [
 *      {
 *        "alternatives": [
 *          {
 *            "transcript": string,
 *            "confidence": number,
 *            "words": [
 *                {
 *                  "startTime": string, "A duration in seconds with up to nine fractional digits, terminated by 's'. Ex; "3.5s"
 *                  "endTime": string,
 *                  "word": string,
 *                },
 *                . . .
 *            ]
 *          },
 *          . . .
 *        ]
 *      },
 *      . . .
 *    ]
 *  }
 * </pre>
 * 
 * Google doc says there is always at least one alternative.
 * <p>
 * If an error occured during the call, the response object contains the error in the "transcript" field for first
 * alternative.
 * 
 * @since 10.2
 */
public class GoogleRESTSpeechToTextResponse implements SpeechToTextResponse {

    protected JSONObject jsonResponse = null;

    protected String cachedText = null;

    protected Double cachedConfidence = null;

    public GoogleRESTSpeechToTextResponse(String httpResponse) throws JSONException {
        try {
            jsonResponse = new JSONObject(httpResponse);
        } catch (JSONException e) {
            buildErrorResponse(e);
        }
    }

    /*
     * {
     *   "results": [
     *     "alternatives": [
     *       {
     *         "transcript": "AN ERROR OCCURED: ...",
     *         "confidence": 0,
     *         "words": []
     *       }
     *     ]
     *   ]
     * }
     */
    protected void buildErrorResponse(Exception exception) throws JSONException {

        jsonResponse = new JSONObject();

        JSONArray results = new JSONArray();
        JSONObject firstResult = new JSONObject();
        JSONArray alternatives = new JSONArray();

        JSONObject errorAlternative = new JSONObject();
        errorAlternative.put("transcript", "AN ERROR OCCURED: " + exception.getMessage());
        errorAlternative.put("confidence", 0);
        JSONArray words = new JSONArray();
        errorAlternative.put("words", words);

        alternatives.put(errorAlternative);
        firstResult.put("alternatives", alternatives);
        results.put(firstResult);
        jsonResponse.put("results", results);

    }

    protected JSONObject getFirstAlternative() throws JSONException {

        JSONObject alternative = null;

        JSONArray results = jsonResponse.getJSONArray("results");
        JSONObject firstResult = results.getJSONObject(0);
        JSONArray alternatives = firstResult.getJSONArray("alternatives");
        alternative = alternatives.getJSONObject(0);

        return alternative;
    }

    protected JSONObject getSecondAlternative() throws JSONException {

        JSONObject alternative = null;

        JSONArray results = jsonResponse.getJSONArray("results");
        if(results != null && results.length() > 1) {
            JSONObject secondResult = results.getJSONObject(1);
            if(secondResult != null) {
                JSONArray alternatives = secondResult.getJSONArray("alternatives");
                alternative = alternatives.getJSONObject(0);
            }
        }
        return alternative;
    }

    protected double parseDuration(String secondsStr) {

        double result = 0.0;

        if (StringUtils.isNotBlank(secondsStr)) {
            // Remove the final "s"
            int pos = secondsStr.indexOf("s");
            if (pos > -1) {
                secondsStr = secondsStr.substring(0, pos);
            }

            pos = secondsStr.indexOf(".");
            if (pos > -1 && secondsStr.length() > (pos + 4)) {
                secondsStr = secondsStr.substring(0, pos + 4);
            }

            result = Double.parseDouble(secondsStr);
        }

        return result;
    }

    /*
     * In this implementation, we get the first result and its first alternative.
     */
    @Override
    public String getText() {

        if (cachedText == null) {
            try {
                JSONObject alternative = getFirstAlternative();
                cachedText = alternative.getString("transcript");
            } catch (JSONException e) {
                throw new NuxeoException("Cannot get the first alternative to read its transcript", e);
            }
        }
        return cachedText;
    }

    @Override
    public double getConfidence() {

        if (cachedConfidence == null) {
            try {
                JSONObject alternative = getFirstAlternative();
                cachedConfidence = alternative.getDouble("confidence");
            } catch (JSONException e) {
                throw new NuxeoException("Cannot get the first alternative to read its confidence", e);
            }
        }
        if (cachedConfidence == null) {
            return -1;
        }
        return cachedConfidence.doubleValue();
    }

    /*
     * Here, we convert to a JSON Array with Double values instead of String for the offsets.
     * It can be big, we don't cache it.
     */
    @Override
    public JSONArray getWordTimeOffsets(boolean withSpeakerTag) throws JSONException {

        JSONArray array = new JSONArray();

        JSONObject alternative = getFirstAlternative();
        JSONArray resultWords = alternative.getJSONArray("words");
        // Speaker can be in the second alternative.
        if(resultWords != null && withSpeakerTag) {
            JSONObject oneResultWord = resultWords.getJSONObject(0);
            if(!oneResultWord.has("speakerTag")) {
                alternative = getSecondAlternative();
                resultWords = alternative.getJSONArray("words");
            }
        }
        if (resultWords != null) {
            JSONObject oneResultWord;
            int max = resultWords.length();
            for (int i = 0; i < max; i++) {
                oneResultWord = resultWords.getJSONObject(i);
                JSONObject obj = new JSONObject();
                obj.put("word", oneResultWord.getString("word"));
                obj.put("start", parseDuration(oneResultWord.getString("startTime")));
                obj.put("end", parseDuration(oneResultWord.getString("endTime")));
                if(withSpeakerTag) {
                    obj.put("speakerTag", oneResultWord.optInt("speakerTag", 0));
                }

                array.put(obj);
            }
        }

        return array;
    }

    @Override
    public Object getNativeResponse() {
        return jsonResponse;
    }

}
