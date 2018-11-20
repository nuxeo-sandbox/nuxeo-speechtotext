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
package org.nuxeo.labs.speechtotext.impl;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.labs.speechtotext.api.SpeechToText;
import org.nuxeo.labs.speechtotext.api.SpeechToTextOptions;
import org.nuxeo.labs.speechtotext.api.SpeechToTextResponse;
import org.nuxeo.labs.speechtotext.google.GoogleSpeechToTextProvider;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This is the implementation of the service.
 * <p>
 * In this implementation we support only Google Speech to Text.
 * For other provider, the service should describe a SpeechToText provider, select it by it's name etc. etc.
 */
public class SpeechToTextImpl extends DefaultComponent implements SpeechToText {

    // In this implementation we support only Google Speech to Text
    GoogleSpeechToTextProvider googleProvider = new GoogleSpeechToTextProvider();

    /**
     * Component activated notification. Called when the component is activated. All component dependencies are resolved
     * at that moment. Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification. Called before a component is unregistered. Use this method to do cleanup if
     * any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification. Called after the application started. You can do here any initialization that
     * requires a working application (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        // do nothing by default. You can remove this method if not used.
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Add some logic here to handle contributions
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        // Logic to do when unregistering any contribution
    }

    @Override
    public SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String languageCode) {

        return googleProvider.run(options, blob, languageCode);
    }

    @Override
    public SpeechToTextResponse run(SpeechToTextOptions options, Blob blob, String audioEncoding, int sampleRateHertz,
            String languageCode) {

        return googleProvider.run(options, blob, audioEncoding, sampleRateHertz, languageCode);
    }

}
