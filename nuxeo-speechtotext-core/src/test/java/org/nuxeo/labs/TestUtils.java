/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.labs;


import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.labs.speechtotext.google.GoogleSpeechToTextProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.2
 */
public class TestUtils {

    protected static int credentialFileTest = -1;

    /**
     * @return true an API key is found either in nuxeo.conf or in the env. variables
     * @since 10.2
     */
    public static boolean loadGoogleCredentials() {

        if (credentialFileTest == -1) {
            credentialFileTest = 0;
            try {
                String apiKey;

                // Check if set in nuxeo.conf (or equivalent) for the test
                apiKey = System.getProperty(GoogleSpeechToTextProvider.API_KEY_PARAM);

                // If not, check the specific env variable.
                if (StringUtils.isBlank(apiKey)) {
                    apiKey = System.getenv(GoogleSpeechToTextProvider.API_KEY_ENV_VAR);
                }
                if (StringUtils.isNotBlank(apiKey)) {
                    Framework.getProperties().setProperty(GoogleSpeechToTextProvider.API_KEY_PARAM, apiKey);
                    credentialFileTest = 1;
                }

            } catch (Exception e) {
                credentialFileTest = 0;
            }
        }

        return credentialFileTest == 1;
    }
    
    public static Blob updateMimetypeIfNeeded(Blob blob) {
        
        if(blob != null && StringUtils.isBlank(blob.getMimeType())) {
            MimetypeRegistryService service = (MimetypeRegistryService) Framework.getService(MimetypeRegistry.class);
            String mimeType = service.getMimetypeFromFile(blob.getFile()); // getMimeTypeFromBlob fails miserably
            blob.setMimeType(mimeType);;
        }
        
        return blob;
        
    }

}
