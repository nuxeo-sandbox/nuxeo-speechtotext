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
import org.nuxeo.labs.api.SpeechToText;
import org.nuxeo.labs.api.SpeechToTextOptions;
import org.nuxeo.labs.api.SpeechToTextResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

/**
 * This is the implementation for Google Speech To text.
 */
public class SpeechToTextImpl extends DefaultComponent implements SpeechToText {

    public static final String API_KEY_PARAM = "google.speechtotext.apikey";

    public static final String API_KEY_ENV_VAR = "GOOGLE_SPEECHTOTEXT_APIKEY";

    /**
     * Component activated notification. Called when the component is activated. All component dependencies are resolved
     * at that moment. Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification. Called before a component is unregistered. Use this method to do cleanup if
     * any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification. Called after the application started. You can do here any initialization that
     * requires a working application (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Add some logic here to handle contributions
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Logic to do when unregistering any contribution
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

    protected boolean isFlacOrWav(Blob blob) {

        String mimeType = blob.getMimeType();
        if (StringUtils.isNotBlank(mimeType)) {
            String mimeTypeLowerCase = mimeType.toLowerCase();
            if (mimeTypeLowerCase.indexOf("flac") > -1 || mimeTypeLowerCase.indexOf("wav") > -1) {
                return false;
            }
        }

        return true;
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

    public SpeechToTextResponse runWithREST(SpeechToTextOptions options, Blob blob, String audioEncoding,
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
            if (isFlacOrWav(blob)) {
                config.put("encoding", audioEncoding);
                config.put("sampleRateHertz", sampleRateHertz);
            }
            jsonBody.put("config", config);

            String bodyJsonStr = jsonBody.toString();

            // ===================================> Call the service
            String url = "https://speech.googleapis.com/v1/speech:recognize?key=" + getGoogleSpeechToTextAPIKey();
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
                response = new SpeechToTextGoogleRESTResponse(responseContent);
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
