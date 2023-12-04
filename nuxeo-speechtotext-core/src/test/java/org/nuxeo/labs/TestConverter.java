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

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.labs.speechtotext.api.SpeechToText;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.labs.nuxeo-speechtotext-core")
public class TestConverter {

    @Inject
    protected ConversionService service;

    @Test
    public void testConverter() {
        
        NuxeoPrincipal p;
        
        p = NuxeoPrincipal.getCurrent();
        DocumentModel dm = p.getModel();
        dm.setPropertyValue("user:company", "toto");
        CoreSession session = dm.getCoreSession();
        session.saveDocument(dm);
        dm.refresh();
        String v = (String) dm.getPropertyValue("user:company");
        
        File audioFile = FileUtils.getResourceFileFromContext("test-audio.aac");
        Blob audioBlob = new FileBlob(audioFile);
        audioBlob = TestUtils.updateMimetypeIfNeeded(audioBlob);
        BlobHolder input = new SimpleBlobHolder(audioBlob);
        BlobHolder result = service.convert(SpeechToText.AUDIO_TO_FLAC_CONVERTER, input, new HashMap<>());
        assertNotNull(result);

        Blob b = result.getBlob();
        assertNotNull(b);
        assertEquals("audio/flac", b.getMimeType());
    }
}
