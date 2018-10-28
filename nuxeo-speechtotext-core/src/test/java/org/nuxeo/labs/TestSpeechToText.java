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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import java.io.File;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.labs.nuxeo-speechtotext-core")
public class TestSpeechToText {

    @Inject
    protected SpeechToText speechToText;

    @Inject
    CoreSession coreSession;

    @Test
    public void testServiceWithBlobToConvert() {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());

        File audioFile = FileUtils.getResourceFileFromContext("output-weather.aac");
        Blob audioBlob = new FileBlob(audioFile);

        // Service will convert to flac
        String transcript = speechToText.run(audioBlob, "en-US");

        assertNotNull(transcript);
        assertTrue(transcript.indexOf("good morning Cleveland") > -1);

    }

    @Test
    public void testService() {

        assumeTrue("Google credentials not found => no test", TestUtils.loadGoogleCredentials());


        Framework.getProperties().setProperty("google.credential.path",
                getClass().getResource("/credential.json").getPath());

        DocumentModel doc = coreSession.createDocumentModel("/", "myFile", "File");
        doc.setPropertyValue("file:content",
                new FileBlob(new File(getClass().getResource("/output-weather.aac").getPath())));

        coreSession.createDocument(doc);
        coreSession.save();

        PathRef docPath = new PathRef(doc.getPathAsString());
        doc = coreSession.getDocument(docPath);

        DocumentModel result = speechToText.transformsText(doc);

        assertNotNull(result);
        assertNotNull(doc.getPropertyValue("dc:description"));
        assertTrue(((String) doc.getPropertyValue("dc:description")).length() > 0);

    }
}
