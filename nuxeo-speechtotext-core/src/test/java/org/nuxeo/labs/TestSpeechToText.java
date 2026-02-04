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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.labs.speechtotext.api.SpeechToText;
import org.nuxeo.labs.speechtotext.api.SpeechToTextOptions;
import org.nuxeo.labs.speechtotext.api.SpeechToTextResponse;
import org.nuxeo.labs.speechtotext.operations.SpeechToTextForDocument;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import java.io.File;
import java.io.Serializable;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.labs.nuxeo-speechtotext-core")
public class TestSpeechToText {

    @Inject
    CoreSession coreSession;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected SpeechToText speechToText;

    @Test
    public void testServiceWithBlobToConvertAndDefaultOptions() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);

        // Service will convert to flac
        SpeechToTextResponse response = speechToText.run(null, audioBlob, "en-US", null);
        assertNotNull(response);

        String transcript = response.getText();
        assertNotNull(transcript);
        assertTrue(transcript.toLowerCase().indexOf("this is john") > -1);

        double confidence = response.getConfidence();
        assertTrue(confidence > 0.5);

    }

    @Test
    public void testWithWordTimeOffsets() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);

        // Service will convert to flac
        SpeechToTextOptions options = new SpeechToTextOptions(false, true);
        SpeechToTextResponse response = speechToText.run(options, audioBlob, "en-US", null);
        assertNotNull(response);

        JSONArray array = response.getWordTimeOffsets(false);
        assertNotNull(array);
        assertTrue(array.length() > 0);

        // Result of cloud provider may change from time to time (getting more accurate for example), so we just search
        // for words in a string instead of searching for an exact start/end for each
        String arrayStr = array.toString().toLowerCase();
        assertTrue(arrayStr.indexOf("\"word\":\"this\"") > -1);
        assertTrue(arrayStr.indexOf("\"word\":\"is\"") > -1);
        assertTrue(arrayStr.indexOf("\"word\":\"john\"") > -1);
        assertTrue(arrayStr.indexOf("\"word\":\"test\"") > -1);

    }

    @Test
    public void testWith2Speakers() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        // Use multi-speaker audio file
        File audioFile = FileUtils.getResourceFileFromContext("test-audio-multispeaker.flac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);

        // No punctuation for this test
        SpeechToTextOptions options = new SpeechToTextOptions(false, false);
        // Set speaker detection
        options.setWithDetectSpeakers(true);
        SpeechToTextResponse response = speechToText.run(options, audioBlob, "en-US", null);
        assertNotNull(response);

        // Let's check it at least get some words from each speaker
        String transcript = response.getText();
        assertNotNull(transcript);

        // Check transcript contains expected content from multi-speaker audio
        // Reminder: punctuation is disabled for this test
        String transcriptLC = transcript.toLowerCase();
        assertTrue(transcriptLC.indexOf("hello this is speaker 1") > -1);

        // Check that word time offsets are returned
        JSONArray array = response.getWordTimeOffsets(true);
        assertNotNull(array);
        assertTrue(array.length() > 0);

        // Verify speakerLabel field exists and has values
        JSONObject firstWord = array.getJSONObject(0);
        assertTrue(firstWord.has("speakerLabel"));
        assertNotNull(firstWord.getString("speakerLabel"));
        assertTrue(!firstWord.getString("speakerLabel").isEmpty());

        // Check that multiple speakers are detected by finding different speaker labels
        String firstSpeaker = firstWord.getString("speakerLabel");
        boolean foundDifferentSpeaker = false;
        for (int i = 1; i < array.length(); i++) {
            JSONObject word = array.getJSONObject(i);
            if (word.has("speakerLabel")) {
                String speaker = word.getString("speakerLabel");
                if (!speaker.equals(firstSpeaker)) {
                    foundDifferentSpeaker = true;
                    break;
                }
            }
        }
        assertTrue("Should detect multiple speakers in multi-speaker audio", foundDifferentSpeaker);

    }

    @Test
    public void testWithMoreOptions() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        // Use multi-speaker audio file
        File audioFile = FileUtils.getResourceFileFromContext("test-audio-multispeaker.flac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);

        // Here we actually override the default configuration with more options.
        String moreOptionsStr = "{\"enableAutomaticPunctuation\": true,";
        moreOptionsStr += "\"enableSpeakerDiarization\": true}";
        JSONObject moreOptions = new JSONObject(moreOptionsStr);

        SpeechToTextResponse response = speechToText.run(null, audioBlob, "en-US", moreOptions);
        assertNotNull(response);

        String transcript = response.getText();
        assertNotNull(transcript);

        // Reminder: punctuation is enabled for this test
        assertTrue(transcript.toLowerCase().indexOf("hello. this is speaker 1.") > -1);
        assertTrue(transcript.indexOf(".") > 0);

        // Check that word time offsets are returned
        JSONArray array = response.getWordTimeOffsets(true);
        assertNotNull(array);
        assertTrue(array.length() > 0);

        // Verify speakerLabel field exists and has values
        JSONObject aWord = array.getJSONObject(0);
        assertTrue(aWord.has("speakerLabel"));
        assertTrue(!aWord.getString("speakerLabel").isEmpty());

    }

    @Test
    public void testDocumentOperation() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        DocumentModel doc = coreSession.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:title", "myFile");
        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);
        doc.setPropertyValue("file:content", (Serializable) audioBlob);

        doc = coreSession.createDocument(doc);
        coreSession.save();

        OperationContext ctx = new OperationContext(coreSession);
        OperationChain chain = new OperationChain("testDocumentOperation");
        // Let default values for blobXpath and saveDocument
        ctx.setInput(doc);
        chain.add(SpeechToTextForDocument.ID).set("transcriptXpath", "dc:description").set("languageCode", "en-US");

        DocumentModel result = (DocumentModel) automationService.run(ctx, chain);

        assertNotNull(result);
        String description = (String) doc.getPropertyValue("dc:description");
        assertNotNull(description);
        assertTrue(description.toLowerCase().indexOf("this is john") > -1);

    }

    @Test
    public void testDocumentOperationWithMoreOptions() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        DocumentModel doc = coreSession.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:title", "myFile");
        File audioFile = FileUtils.getResourceFileFromContext("test-audio-multispeaker.flac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);
        doc.setPropertyValue("file:content", (Serializable) audioBlob);

        doc = coreSession.createDocument(doc);
        coreSession.save();

        OperationContext ctx = new OperationContext(coreSession);
        OperationChain chain = new OperationChain("testDocumentOperation");
        // Let default values for blobXpath and saveDocument
        ctx.setInput(doc);

        String moreOptionsStr = "{\"enableAutomaticPunctuation\": true,";
        moreOptionsStr += "\"enableSpeakerDiarization\": true}"; // With speaker detection
        chain.add(SpeechToTextForDocument.ID)
             .set("transcriptXpath", "dc:description")
             .set("languageCode", "en-US")
             .set("moreOptionsJSONStr", moreOptionsStr)
             .set("resultVarName", "theResult");

        DocumentModel result = (DocumentModel) automationService.run(ctx, chain);

        assertNotNull(result);
        String description = (String) doc.getPropertyValue("dc:description");
        assertNotNull(description);
        assertTrue(description.toLowerCase().indexOf("hello. this is speaker 1.") > -1);

        // Punctuation is enabled
        assertTrue(description.indexOf(".") > 0);

        // Check native response
        SpeechToTextResponse response = (SpeechToTextResponse) ctx.get("theResult");
        assertNotNull(response);

        // Check that word time offsets with speaker info are returned
        JSONArray array = response.getWordTimeOffsets(true);
        assertNotNull(array);
        assertTrue(array.length() > 0);

        // Verify speakerLabel field exists and has values
        JSONObject aWord = array.getJSONObject(0);
        assertTrue(aWord.has("speakerLabel"));
        assertTrue(!aWord.getString("speakerLabel").isEmpty());

    }
}
