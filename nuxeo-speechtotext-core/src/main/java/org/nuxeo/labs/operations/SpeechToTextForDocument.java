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
package org.nuxeo.labs.operations;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.api.SpeechToText;
import org.nuxeo.labs.api.SpeechToTextOptions;
import org.nuxeo.labs.api.SpeechToTextResponse;

/**
 *
 */
@Operation(id = SpeechToTextForDocument.ID, category = Constants.CAT_CONVERSION, label = "Speech to Text", description = "Send the blob found in blobXpath (default file:content) to SpeechToText, using the languageCode."
        + " Return the transcript in the transcriptXpath field. Optionaly save the document (default false)"
        + " If resultVarName is not empty this context variable is set to the SpeechToTextResponse object which has more accessors")
public class SpeechToTextForDocument {

    public static final String ID = "Convert.SpeechToTextOp";

    @Context
    protected CoreSession session;
    
    @Context
    protected OperationContext ctx;

    @Context
    protected SpeechToText speechToText;

    @Param(name = "blobXpath", required = false, values = { "file:content" })
    protected String blobXpath = "file:content";

    @Param(name = "transcriptXpath", required = true)
    protected String transcriptXpath;

    @Param(name = "languageCode", required = true, values = { "en-US" })
    protected String languageCode = "en-US";

    @Param(name = "withPunctuation", required = false, values = { "true" })
    protected boolean withPunctuation = true;

    @Param(name = "withWordTimeOffets", required = false, values = { "false" })
    protected boolean withWordTimeOffets = false;

    @Param(name = "saveDocument", required = false, values = { "false" })
    protected boolean saveDocument = false;
    
    @Param(name = "resultVarName", required = false)
    protected String resultVarName;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {

        String transcript = null;
        SpeechToTextResponse response = null;

        Blob blob = (Blob) input.getPropertyValue(blobXpath);

        if (blob != null) {
            response = speechToText.run(new SpeechToTextOptions(withPunctuation, withWordTimeOffets), blob, languageCode);
            transcript = response.getText();
        }

        input.setPropertyValue(transcriptXpath, transcript);
        
        if (StringUtils.isNotBlank(resultVarName)) {
            ctx.put(resultVarName, response);
        }

        if (saveDocument) {
            input = session.saveDocument(input);
        }

        return input;

    }
}