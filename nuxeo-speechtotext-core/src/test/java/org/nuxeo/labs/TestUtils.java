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

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.2
 */
public class TestUtils {

    // Relative to the test "resources" folder
    public static final String CREDENTIALS_FILE_TEST_PATH = "credentials.json";

    protected static int credentialFileTest = -1;

    /**
     * @return true if path to credentials is fooun is available
     * @since 10.2
     */
    public static boolean loadGoogleCredentials() {

        File credentialsJson = null;

        if (credentialFileTest == -1) {
            credentialFileTest = 0;
            try {
                String credentialsFilePath;

                // Check if set in nuxeo.conf (or equivalent) for the test
                credentialsFilePath = System.getProperty(SpeechToTextImpl.CREDENTIALS_PATH_PARAM);

                // If not, check the specific env variable.
                if (StringUtils.isBlank(credentialsFilePath)) {
                    credentialsFilePath = System.getenv(SpeechToTextImpl.CREDENTIALS_PATH_ENV_VAR);

                    // Still not there, try the local test file
                    if (StringUtils.isBlank(credentialsFilePath)) {
                        credentialsJson = FileUtils.getResourceFileFromContext(CREDENTIALS_FILE_TEST_PATH);
                        // We are here => no error, there is a CREDENTIAL_FILE_PATH file
                        credentialsFilePath = credentialsJson.getAbsolutePath();
                    }
                }
                if (StringUtils.isNotBlank(credentialsFilePath)) {
                    Framework.getProperties().setProperty(SpeechToTextImpl.CREDENTIALS_PATH_PARAM, credentialsFilePath);
                    credentialFileTest = 1;
                }

            } catch (Exception e) {
                credentialFileTest = 0;
            }
        }

        return credentialFileTest == 1;
    }

}
