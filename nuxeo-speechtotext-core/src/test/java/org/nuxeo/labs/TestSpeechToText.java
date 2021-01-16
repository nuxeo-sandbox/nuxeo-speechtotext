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
import org.json.JSONException;
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
import java.util.Iterator;

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
        assertTrue(arrayStr.indexOf("\"word\":\"french\"") > -1);

    }

    @Test
    public void testWith2Speakers() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        // SOP far we just have the single speaker audio. Still using it.
        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);

        // Service will convert to flac
        // No punctuation for this test
        SpeechToTextOptions options = new SpeechToTextOptions(false, false);
        // Set speaker detection
        options.setWithDetectSpeakers(true);
        SpeechToTextResponse response = speechToText.run(options, audioBlob, "en-US", null);
        assertNotNull(response);

        // As of "today" (writing of this test, 2018-11), Google API does not detect 2 different speakers...
        // Let's check it at least get some works from each speaker
        String transcript = response.getText();
        assertNotNull(transcript);

        String transcriptLC = transcript.toLowerCase();
        assertTrue(transcriptLC.indexOf("this is john") > -1);

        // Still, check there is at least one speaker
        JSONArray array = response.getWordTimeOffsets(true);
        assertNotNull(array);
        assertTrue(array.length() > 0);
        JSONObject aWord = array.getJSONObject(0);
        assertTrue(aWord.has("speakerTag"));

    }

    @Test
    public void testWithMoreOptions() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);

        // Here we actually override the default config + add speakers
        String moreOptionsStr = "{\"enableAutomaticPunctuation\": false,"; // No punctuation
        moreOptionsStr += "\"enableSpeakerDiarization\": true}"; // and with speaker detection
        JSONObject moreOptions = new JSONObject(moreOptionsStr);

        // Service will convert to flac
        SpeechToTextResponse response = speechToText.run(null, audioBlob, "en-US", moreOptions);
        assertNotNull(response);

        String transcript = response.getText();
        assertNotNull(transcript);

        assertTrue(transcript.toLowerCase().indexOf("this is john") > -1);
        // No punctuation
        assertTrue(transcript.indexOf(".") < 0);
        assertTrue(transcript.indexOf(",") < 0);

        // There is at least one speaker
        JSONArray array = response.getWordTimeOffsets(true);
        assertNotNull(array);
        assertTrue(array.length() > 0);
        JSONObject aWord = array.getJSONObject(0);
        assertTrue(aWord.has("speakerTag"));
        assertEquals(aWord.getInt("speakerTag"), 1);

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

        String moreOptionsStr = "{\"enableAutomaticPunctuation\": false,"; // No punctuation
        moreOptionsStr += "\"enableSpeakerDiarization\": true}"; // and with speaker detection
        chain.add(SpeechToTextForDocument.ID)
             .set("transcriptXpath", "dc:description")
             .set("languageCode", "en-US")
             .set("moreOptionsJSONStr", moreOptionsStr)
             .set("resultVarName", "theResult");

        DocumentModel result = (DocumentModel) automationService.run(ctx, chain);

        assertNotNull(result);
        String description = (String) doc.getPropertyValue("dc:description");
        assertNotNull(description);
        assertTrue(description.toLowerCase().indexOf("this is john") > -1);

        // No punctuation
        assertTrue(description.indexOf(".") < 0);
        assertTrue(description.indexOf(",") < 0);
        
        // Check native response
        SpeechToTextResponse response = (SpeechToTextResponse) ctx.get("theResult");
        assertNotNull(response);
        
        // There is at least one speaker
        JSONArray array = response.getWordTimeOffsets(true);
        assertNotNull(array);
        assertTrue(array.length() > 0);
        JSONObject aWord = array.getJSONObject(0);
        assertTrue(aWord.has("speakerTag"));
        assertEquals(aWord.getInt("speakerTag"), 1);

    }
}
