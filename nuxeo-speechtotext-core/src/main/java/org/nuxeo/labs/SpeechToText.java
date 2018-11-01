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
package org.nuxeo.labs;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;

public interface SpeechToText {

    /*
     * Convert and audio file fo FLAC. If the file is a video, extracts the audio to FLAC. (See the command line XML
     * contribution)
     */
    public static final String AUDIO_TO_FLAC_CONVERTER = "audio-to-flac";

    /**
     * Returns the transcript of the audio file.<br>
     * This will always convert the input blob to FLAC before sending it to the cloud service, possibly using the
     * <code>AUDIO_TO_FLAC_CONVERTER</code> converter provided by the service (depends on the implementation)<br>
     *
     * @param Blob audio file, or at least a file that can be converted to audio
     * @param languageCode of the audio file
     * @return string of text
     */
    String run(Blob blob, String languageCode);

    /**
     * Returns the transcript of the audio file.<br>
     * The blob must be an audio, or at least a file compatible with the speech to text cloud service
     *
     * @param Blob audio file
     * @param audioEncoding of the audio file
     * @param sampleRateHertz of the audio file
     * @param languageCode of the audio file
     * @return string of text
     */
    String run(Blob blob, String audioEncoding, int sampleRateHertz, String languageCode);

    /**
     * Helper that returns the RecognitionConfig.AudioEncoding value for the name of the enum.<br>
     * (Useful for Operations, where a caller will pass a string, not an enum value)
     *
     * @param valueStr
     * @return the enum value for the input string
     * @since 10.2
     */
    static public RecognitionConfig.AudioEncoding EncodingNameToEnum(String valueStr) {

        AudioEncoding value = AudioEncoding.UNRECOGNIZED;
        try {
            value = AudioEncoding.valueOf(valueStr);
        } catch (IllegalArgumentException e) {
            valueStr = valueStr.toUpperCase();
            try {
                value = AudioEncoding.valueOf(valueStr);
            } catch (IllegalArgumentException e2) {
                throw new NuxeoException("Unrecognized AudioEncoding value: " + valueStr);
            }
        }
        return value;
    }
}
