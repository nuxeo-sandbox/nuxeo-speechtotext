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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.labs.speechtotext.api.SpeechToTextOptions;
import org.nuxeo.labs.speechtotext.api.SpeechToTextProvider;
import org.nuxeo.labs.speechtotext.api.SpeechToTextResponse;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * @since 10.2
 */
public class GoogleSpeechToTextProvider implements SpeechToTextProvider {
    
    public static final String SPEECHTOTEXT_API_VERSION = "v1p1beta1";

    public static final String API_KEY_PARAM = "google.speechtotext.apikey";

    public static final String API_KEY_ENV_VAR = "GOOGLE_SPEECHTOTEXT_APIKEY";
    
    public GoogleSpeechToTextProvider() {
        
    }

    @Override
    public SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String languageCode) {

        Blob normalized = normalizeAudio(blob);

        return run(options, normalized, null, -1, languageCode);
    }

    @Override
    public SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String audioEncoding, int sampleRateHertz,
            String languageCode) {

        return runWithREST(options, blob, audioEncoding, sampleRateHertz, languageCode);

    }
    
    protected String getGoogleSpeechToTextAPIKey() {

        String apiKey = Framework.getProperty(API_KEY_PARAM);
        if (StringUtils.isBlank(apiKey)) {
            apiKey = System.getenv(API_KEY_ENV_VAR);
        }

        if (StringUtils.isBlank(apiKey)) {
            throw new NuxeoException("API Keyfor Google Speech To Text APi not found in nuxeo.conf (" + API_KEY_PARAM
                    + ") nor in an environement variable (" + API_KEY_ENV_VAR + ")");
        }

        return apiKey;
    }

    protected boolean isFlacOrWav(Blob blob) {

        String mimeType = blob.getMimeType();
        if (StringUtils.isNotBlank(mimeType)) {
            String mimeTypeLowerCase = mimeType.toLowerCase();
            if (mimeTypeLowerCase.indexOf("flac") > -1 || mimeTypeLowerCase.indexOf("wav") > -1) {
                return true;
            }
        }

        return false;
    }

    protected Blob normalizeAudio(Blob blob) {

        if (isFlacOrWav(blob)) {
            return blob;
        }

        ConversionService service = Framework.getService(ConversionService.class);

        BlobHolder blobholder = new SimpleBlobHolder(blob);

        BlobHolder result = service.convert(AUDIO_TO_FLAC_CONVERTER, blobholder, new HashMap<>());

        return result.getBlob();

    }
    
    protected SpeechToTextResponse runWithREST(SpeechToTextOptions options, Blob blob, String audioEncoding,
            int sampleRateHertz, String languageCode) {

        SpeechToTextResponse response = null;

        if (options == null) {
            options = SpeechToTextOptions.buildDefaultOptions();
        }

        CloseableHttpClient client = HttpClients.createDefault();

        try {

            // ===================================> Convert audio to Base64 String
            byte[] data = blob.getByteArray();
            String audioStr = Base64.getEncoder().encodeToString(data);

            // ===================================> Setup the body (JSON)
            JSONObject jsonBody = new JSONObject();

            // -----------------> Audio (a file sent as Base64, not a uri to a file in Google Storage)
            JSONObject audioJson = new JSONObject();
            audioJson.put("content", audioStr);
            jsonBody.put("audio", audioJson);

            // -----------------> Configuration
            JSONObject config = new JSONObject();
            config.put("languageCode", languageCode);
            config.put("enableAutomaticPunctuation", options.isWithPunctuation());
            config.put("enableWordTimeOffsets", options.isWithWordTimeOffsets());
            /*
             * Google Cloud doc (2018-10-28): << You are not required to specify the encoding and sample rate for WAV or
             * FLAC files. If omitted, Speech-to-Text automatically determines the encoding and sample rate for WAV or
             * FLAC files based on the file header. If you specify an encoding or sample rate value that does not match
             * the value in the file header, then Speech-to-Text returns an error. >>
             */
            if (!isFlacOrWav(blob)) {
                config.put("encoding", audioEncoding);
                config.put("sampleRateHertz", sampleRateHertz);
            }
            jsonBody.put("config", config);

            String bodyJsonStr = jsonBody.toString();

            // ===================================> Call the service
            String url = "https://speech.googleapis.com/" + SPEECHTOTEXT_API_VERSION + "/speech:recognize?key=" + getGoogleSpeechToTextAPIKey();
            HttpPost httpPost = new HttpPost(url);

            StringEntity bodyEntity = new StringEntity(bodyJsonStr);
            httpPost.setEntity(bodyEntity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            CloseableHttpResponse httpResponse = client.execute(httpPost);

            // ===================================> Get the result
            int httpResponsecode = httpResponse.getStatusLine().getStatusCode();
            if (httpResponsecode != 200) {
                throw new NuxeoException("Problem calling the service, Status code: " + httpResponsecode + ", "
                        + httpResponse.getStatusLine().getReasonPhrase());
            } else {
                HttpEntity responseEntity = httpResponse.getEntity();
                BufferedHttpEntity buf = new BufferedHttpEntity(responseEntity);
                String responseContent = EntityUtils.toString(buf, StandardCharsets.UTF_8);
                response = new GoogleRESTSpeechToTextResponse(responseContent);
            }

        } catch (IOException | JSONException e) {
            throw new NuxeoException("Error getting the speech-to-text result.", e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // Ignore;
            }
        }

        return response;
    }

}
