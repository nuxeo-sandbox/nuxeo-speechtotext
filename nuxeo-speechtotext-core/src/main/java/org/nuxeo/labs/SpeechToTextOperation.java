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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 */
@Operation(id = SpeechToTextOperation.ID, category = Constants.CAT_CONVERSION, label = "Speech to Text", description = "Send the blob found in blobXpath (default file:content) to SpeechToText, using the languageCode."
        + " Return the transcript in the transcriptXpath field. Optionaly save the document (default false)")
public class SpeechToTextOperation {

    public static final String ID = "Convert.SpeechToTextOp";

    @Context
    protected CoreSession session;

    @Context
    protected SpeechToText speechToText;

    @Param(name = "blobXpath", required = false, values = { "file:content" })
    protected String blobXpath = "file:content";

    @Param(name = "transcriptXpath", required = true)
    protected String transcriptXpath;

    @Param(name = "languageCode", required = true, values = { "en-US" })
    protected String languageCode = "en-US";

    @Param(name = "saveDocument", required = false, values = { "false" })
    protected boolean saveDocument = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {

        String transcript = null;

        Blob blob = (Blob) input.getPropertyValue(blobXpath);

        if (blob != null) {
            transcript = speechToText.run(blob, languageCode);
        }

        input.setPropertyValue(transcriptXpath, transcript);

        if (saveDocument) {
            input = session.saveDocument(input);
        }

        return input;

    }
}
