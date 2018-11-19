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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.labs.api.SpeechToText;
import org.nuxeo.labs.api.SpeechToTextOptions;
import org.nuxeo.labs.api.SpeechToTextResponse;

/**
 *
 */
@Operation(id = SpeechToTextForBlob.ID, category = Constants.CAT_CONVERSION, label = "Blob: Speech to Text", description = "Send the inoput blob to the SpeechToText service, using the languageCode."
        + " The resultVarName context variable is set to the SpeechToTextResponse object which has more accessors."
        + " If the blob is a FLAC audio file, audioEncoding and sampleRateHertz are optional and can be ommited."
        + " If passed, audioEncoding must be a valid value of RecognitionConfig.AudioEncoding (LINEAR16, FLAC, MULAW ..."
        + " Misc. options can be set (punctuation, wordTimeOffsets.)"
        + " The operaiton returns the input blob unchanged")
public class SpeechToTextForBlob {

    public static final String ID = "Convert.SpeechToTextForBlob";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected SpeechToText speechToText;

    @Param(name = "languageCode", required = true, values = { "en-US" })
    protected String languageCode = "en-US";

    @Param(name = "audioEncoding", required = false)
    protected String audioEncoding;

    @Param(name = "sampleRateHertz", required = false)
    protected int sampleRateHertz;

    @Param(name = "withPunctuation", required = false)
    protected boolean withPunctuation = false;

    @Param(name = "withWordTimeOffets", required = false)
    protected boolean withWordTimeOffets = false;

    @Param(name = "resultVarName", required = true)
    protected String resultVarName;

    @OperationMethod
    public Blob run(Blob input) {

        SpeechToTextOptions options = new SpeechToTextOptions(withPunctuation, withWordTimeOffets);

        SpeechToTextResponse response = speechToText.run(options, input, audioEncoding, sampleRateHertz, languageCode);

        ctx.put(resultVarName, response);

        return input;

    }
}