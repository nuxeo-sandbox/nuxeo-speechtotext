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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * @since 10.2
 */
public class TestUtils {

    // Relative to the test "resources" folder
    public static final String CREDENTIAL_FILE_PATH = "credential.json";

    protected static int credentialFileTest = -1;

    /**
     *
     * @return true if the /credential.json file is available
     * @since 10.2
     */
    public static boolean loadGoogleCredentials() {

        File credentialsJson = null;

        if(credentialFileTest == -1) {
            try {
                credentialsJson = FileUtils.getResourceFileFromContext(CREDENTIAL_FILE_PATH);
                // We are here => no error
                Framework.getProperties().setProperty("google.credential.path", credentialsJson.getAbsolutePath());
                credentialFileTest = 1;
            } catch (Exception e) {
                credentialFileTest = 0;
            }
        }

        return credentialFileTest == 1;
    }

}
