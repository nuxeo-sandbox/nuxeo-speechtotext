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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
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
import org.nuxeo.labs.api.SpeechToText;
import org.nuxeo.labs.api.SpeechToTextOptions;
import org.nuxeo.labs.api.SpeechToTextResponse;
import org.nuxeo.labs.operations.SpeechToTextForDocument;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import java.io.File;

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
    public void testServiceWithBlobToConvert() {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);

        // Service will convert to flac
        SpeechToTextResponse response = speechToText.run(null, audioBlob, "en-US");
        assertNotNull(response);
        String transcript = response.getText();

        assertNotNull(transcript);
        assertTrue(transcript.toLowerCase().indexOf("this is john") > -1);

    }

    @Test
    public void testWithWordTimeOffsets() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);

        // Service will convert to flac
        SpeechToTextOptions options = new SpeechToTextOptions(false, true);
        SpeechToTextResponse response = speechToText.run(options, audioBlob, "en-US");
        assertNotNull(response);

        JSONArray array = response.getWordTimeOffsets();
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
    public void testDocumentOperation() throws Exception {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        DocumentModel doc = coreSession.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("dc:title", "myFile");
        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        doc.setPropertyValue("file:content", new FileBlob(audioFile));

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
}
