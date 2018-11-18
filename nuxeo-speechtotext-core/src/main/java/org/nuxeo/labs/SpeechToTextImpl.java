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

import com.google.common.collect.Lists;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

/**
 * This is the implementation for Google Speech To text.
 */
public class SpeechToTextImpl extends DefaultComponent implements SpeechToText {

    public static final String CREDENTIALS_PATH_PARAM = "google.speechtotext.credentials";

    public static final String CREDENTIALS_PATH_ENV_VAR = "GOOGLE_SPEECHTOTEXT_CREDENTIALS";

    protected volatile SpeechClient speechClient = null;

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

    protected SpeechClient getSpeechClient() {

        if (speechClient == null) {
            synchronized (this) {
                if (speechClient == null) {
                    GoogleCredentials credentials = getCredentials();
                    try {
                        SpeechSettings speechSettings = SpeechSettings.newBuilder()
                                                                      .setCredentialsProvider(
                                                                              FixedCredentialsProvider.create(
                                                                                      credentials))
                                                                      .build();
                        speechClient = SpeechClient.create(speechSettings);
                    } catch (IOException e) {
                        throw new NuxeoException("Error getting the Speech-to-Text client", e);
                    }
                }
            }
        }

        return speechClient;
    }

    protected GoogleCredentials getCredentials() {

        String credentialsFilePath = Framework.getProperty(CREDENTIALS_PATH_PARAM);
        if (StringUtils.isBlank(credentialsFilePath)) {
            credentialsFilePath = System.getenv(CREDENTIALS_PATH_ENV_VAR);
        }
        if (StringUtils.isBlank(credentialsFilePath)) {
            throw new NuxeoException("Credentials file for Google SPeech To Text APi not found in nuxeo.conf ("
                    + CREDENTIALS_PATH_PARAM + ") nor in an environement variable (" + CREDENTIALS_PATH_ENV_VAR + ")");
        }

        GoogleCredentials credentials = null;
        try {
            credentials = GoogleCredentials.fromStream(new FileInputStream(new File(credentialsFilePath)))
                                           .createScoped(Lists.newArrayList(
                                                   "https://www.googleapis.com/auth/cloud-platform"));
            return credentials;
        } catch (IOException e) {
            throw new NuxeoException("Error building Google Credentials", e);
        }

    }

    @Override
    public SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String languageCode) {

        Blob normalized = normalizeAudio(blob);

        return run(options, normalized, null, -1, languageCode);
    }

    @Override
    public SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String audioEncoding, int sampleRateHertz,
            String languageCode) {

        try {
            
            if(languageCode != null) {
            //return runWithREST(options, blob, audioEncoding, sampleRateHertz, languageCode);
            }

            if (options == null) {
                options = SpeechToTextOptions.buildDefaultOptions();
            }

            // Reads the audio file into memory
            byte[] data = blob.getByteArray();
            ByteString audioBytes = ByteString.copyFrom(data);
            RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

            /*
             * Google Cloud doc (2018-10-28): << You are not required to specify the encoding and sample rate for WAV or
             * FLAC files. If omitted, Speech-to-Text automatically determines the encoding and sample rate for WAV or
             * FLAC files based on the file header. If you specify an encoding or sample rate value that does not match
             * the value in the file header, then Speech-to-Text returns an error. >>
             */
            String mimeType = blob.getMimeType();
            boolean needsParameters = true;
            if (StringUtils.isNotBlank(mimeType)) {
                String mimeTypeLowerCase = mimeType.toLowerCase();
                needsParameters = mimeTypeLowerCase.indexOf("flac") < 0 && mimeTypeLowerCase.indexOf("wav") < 0;
            }

            RecognitionConfig.Builder builder;
            // Shared parameters
            builder = RecognitionConfig.newBuilder().setLanguageCode(languageCode);
            // Specific
            builder = builder.setEnableAutomaticPunctuation(options.isWithPunctuation());
            builder = builder.setEnableWordTimeOffsets(options.isWithWordTimeOffsets());
            if (needsParameters) {
                RecognitionConfig.AudioEncoding encoding = SpeechToText.EncodingNameToEnum(audioEncoding);
                builder = builder.setEncoding(encoding).setSampleRateHertz(sampleRateHertz);
            }
            RecognitionConfig config = builder.build();

            // Performs speech recognition on the audio file
            RecognizeResponse response = getSpeechClient().recognize(config, audio);

            return new SpeechToTextGoogleResponse(response);

        } catch (IOException e) {
            throw new NuxeoException("Error getting the Speech-to-Text result", e);
        }
    }

    protected Blob normalizeAudio(Blob input) {

        ConversionService service = Framework.getService(ConversionService.class);

        BlobHolder blobholder = new SimpleBlobHolder(input);

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

            // Convert audio to Base64 String
            byte[] data = blob.getByteArray();
            String audioStr = Base64.getEncoder().encodeToString(data);

            JSONObject jsonBody = new JSONObject();
            
            JSONObject audioJson = new JSONObject();
            audioJson.put("content", audioStr);
            jsonBody.put("audio", audioJson);

            /*
             * "config": { "encoding":"FLAC", "sampleRateHertz": 16000, "languageCode": "en-US",
             * "enableAutomaticPunctuation": true, "enableWordTimeOffsets": false },
             */

            JSONObject config = new JSONObject();
            config.put("languageCode", languageCode);
            config.put("enableAutomaticPunctuation", options.isWithPunctuation());
            config.put("enableWordTimeOffsets", options.isWithWordTimeOffsets());
            jsonBody.put("config", config);
            
            String bodyJsonStr = jsonBody.toString();

            String apiKey = System.getenv("GOOGLE_SPEECHTOTEXT_APIKEY");
            String url = "https://speech.googleapis.com/v1/speech:recognize?key=" + apiKey;
            HttpPost httpPost = new HttpPost(url);
            
            StringEntity bodyEntity = new StringEntity(bodyJsonStr);
            httpPost.setEntity(bodyEntity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse httpResponse = client.execute(httpPost);
            
            int httpResponsecode = httpResponse.getStatusLine().getStatusCode();
            HttpEntity responseEntity = httpResponse.getEntity();
            BufferedHttpEntity buf = new BufferedHttpEntity(responseEntity);
            String responseContent = EntityUtils.toString(buf, StandardCharsets.UTF_8);
            System.out.println("\n" + responseContent + "\n");
            
        } catch (Exception e) {

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
