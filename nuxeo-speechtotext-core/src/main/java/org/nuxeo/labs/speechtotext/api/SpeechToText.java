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
 *     Eliot Kim
 *     Thibaud Arguillere
 */
package org.nuxeo.labs.speechtotext.api;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.Blob;

public interface SpeechToText {

    /*
     * Convert and audio file to FLAC. If the file is a video, extracts the audio to FLAC. (See the command line XML
     * contribution)
     */
    public static final String AUDIO_TO_FLAC_CONVERTER = "audio-to-flac";

    /**
     * Returns the transcript of the audio file.<br>
     * This will convert the input blob to FLAC before sending it to the cloud service, possibly using the
     * <code>AUDIO_TO_FLAC_CONVERTER</code> converter provided by the service (depends on the implementation)<br>
     * This automatic conversion is performed only if needed (when using Google, if the audio is flac or wav, no
     * conversion is needed)
     * <p>
     * <code>moreOptions</code> allows for sending more configuration parameters to the provider than exposed in the
     * code, so all the features of the provider can be used when the API evolves, without having to wait for this code
     * to be upgraded. The result will always have to be handled via <code>SpeechToTextResponse#getNativeResponse</code>.
     * What can be sent is to be get from the provider API documentation.
     *
     * @param options, the options when calling the service. If null, default options apply see
     *            {@link SpeechToTextOptions}
     * @param Blob audio file, or at least a file that can be converted to audio
     * @param languageCode of the audio file
     * @param moreOptions to add to the configuration sent to the provider
     * @return string of text
     */
    SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String languageCode, JSONObject moreOptions);

    /**
     * Returns the transcript of the audio file.<br>
     * The blob must be an audio, or at least a file compatible with the speech to text cloud service
     * <p>
     * WARNING: audioEncoding must be the exact value as expected by the provider. For Google (first provider
     * implemented), check
     * https://cloud.google.com/speech-to-text/docs/reference/rest/{VERSION_USED}/RecognitionConfig#AudioEncoding
     * <p>
     * <code>moreOptions</code> allows for sending more configuration parameters to the provider than exposed in the
     * code, so all the features of the provider can be used when the API evolves, without having to wait for this code
     * to be upgraded. The result will always have to be handled via <code>SpeechToTextResponse#getNativeResponse</code>.
     * What can be sent is to be get from the provider API documentation.
     * 
     * @param options, the options when calling the service. If null, default options apply see
     *            {@link SpeechToTextOptions}
     * @param Blob audio file
     * @param audioEncoding of the audio file
     * @param sampleRateHertz of the audio file
     * @param languageCode of the audio file
     * @param moreOptions to add to the configuration sent to the provider
     * @return the response from the provider
     */
    SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String audioEncoding, int sampleRateHertz,
            String languageCode, JSONObject moreOptions);

    /**
     * Utility to allow for merging <code>toMerge</code> into <code>dest</code>
     * 
     * @param dest
     * @param toMerge
     * @return
     * @throws JSONException
     */
    static public JSONObject mergeJSONObjects(JSONObject dest, JSONObject toMerge) throws JSONException {

        if (dest == null || toMerge == null) {
            return dest;
        }

        @SuppressWarnings("unchecked")
        Iterator<String> keys = toMerge.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = toMerge.get(key);
            dest.put(key, value);
            /*
            if (value instanceof String) {
                dest.put(key, (String) value);
            } else if (value instanceof Double) {
                dest.put(key, ((Double) value).doubleValue());
            } else if (value instanceof Integer) {
                dest.put(key, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                dest.put(key, ((Long) value).longValue());
            } else if (value instanceof Boolean) {
                dest.put(key, ((Boolean) value).booleanValue());
            }
            */

        }

        return dest;
    }

}
