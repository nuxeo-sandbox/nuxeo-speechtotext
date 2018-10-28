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
 */
package org.nuxeo.labs;

import com.google.common.collect.Lists;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpeechToTextImpl extends DefaultComponent implements SpeechToText {

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

    protected GoogleCredentials getCredentials() {

        String path = Framework.getProperty("google.credential.path");
        GoogleCredentials credentials = null;

        try {
            credentials = GoogleCredentials.fromStream(new FileInputStream(new File(path))).createScoped(
                    Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            return credentials;
        } catch (IOException e) {
            throw new NuxeoException("Error building Google Credentials", e);
        }

    }

    @Override
    public String run(Blob blob, String languageCode) {

        Blob normalized = normalizeAudio(blob);

        return run(normalized, "FLAC", 44100, languageCode);
    }

    @Override
    public String run(Blob blob, String audioEncoding, int sampleRateHertz, String languageCode) {

        ArrayList<String> finalResult = null;

        GoogleCredentials credentials = getCredentials();

        try {
            SpeechSettings speechSettings = SpeechSettings.newBuilder()
                                                          .setCredentialsProvider(
                                                                  FixedCredentialsProvider.create(credentials))
                                                          .build();
            SpeechClient speechClient = SpeechClient.create(speechSettings);

            finalResult = new ArrayList<String>();

            // Reads the audio file into memory
            byte[] data = blob.getByteArray();
            ByteString audioBytes = ByteString.copyFrom(data);

            RecognitionConfig.AudioEncoding encoding = SpeechToText.EncodingNameToEnum(audioEncoding);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                                                        .setEncoding(encoding)
                                                        .setSampleRateHertz(sampleRateHertz)
                                                        .setLanguageCode(languageCode)
                                                        .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
            // Performs speech recognition on the audio file
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            for (SpeechRecognitionResult result : results) {
                // There can be several alternative transcripts for a given chunk of speech. Just use the
                // first (most likely) one here.
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                finalResult.add(alternative.getTranscript());
                // System.out.printf("Transcription: %s%n", alternative.getTranscript());
            }

            return finalResult.get(0);

        } catch (IOException e) {
            throw new NuxeoException("Error getting the Speech-to-Text result", e);
        }
    }

    @Override
    public DocumentModel transformsText(DocumentModel doc) {

        Blob blob = (Blob) doc.getPropertyValue("file:content");
        Blob normalized = normalizeAudio(blob);

        ArrayList<String> finalResult = null;
        try {
            String path = Framework.getProperty("google.credential.path");
            // sets user credentials through the .json file so that it doesnt have to be set in terminal
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(new File(path)))
                                                             .createScoped(Lists.newArrayList(
                                                                     "https://www.googleapis.com/auth/cloud-platform"));
            SpeechSettings speechSettings = SpeechSettings.newBuilder()
                                                          .setCredentialsProvider(
                                                                  FixedCredentialsProvider.create(credentials))
                                                          .build();
            SpeechClient speechClient = SpeechClient.create(speechSettings);

            // The path to the audio file to transcribe
            // String fileName = input;

            // to store final results
            finalResult = new ArrayList<String>();

            // Reads the audio file into memory
            byte[] data = normalized.getByteArray();
            ByteString audioBytes = ByteString.copyFrom(data);

            RecognitionConfig config = RecognitionConfig.newBuilder()
                                                        .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                                                        .setSampleRateHertz(44100)
                                                        .setLanguageCode("en-US")
                                                        .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();
            // Performs speech recognition on the audio file
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            for (SpeechRecognitionResult result : results) {
                // There can be several alternative transcripts for a given chunk of speech. Just use the
                // first (most likely) one here.
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                finalResult.add(alternative.getTranscript());
                // System.out.printf("Transcription: %s%n", alternative.getTranscript());
            }
            doc.setPropertyValue("dc:description", finalResult.get(0));

        } catch (IOException e) {
            e.printStackTrace();
        }
        // should return a Document Model
        return doc;

    };

    protected Blob normalizeAudio(Blob input) {
        ConversionService service = Framework.getService(ConversionService.class); // can get service names from
                                                                                   // explorer.nuxeo
        BlobHolder blobholder = new SimpleBlobHolder(input);

        BlobHolder result = service.convert("normalize-audio", blobholder, new HashMap<>());

        return result.getBlob();

    };

}
