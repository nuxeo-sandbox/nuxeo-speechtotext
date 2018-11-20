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
 *     Thibaud Arguillere
 */
package org.nuxeo.labs.speechtotext.api;

/**
 * Utility class to setup options when calling the service.
 * 
 * @since 10.2
 */
public class SpeechToTextOptions {

    protected boolean withPunctuation;

    protected boolean withWordTimeOffsets;

    public SpeechToTextOptions(boolean withPunctuation, boolean withWordTimeOffsets) {
        super();
        this.withPunctuation = withPunctuation;
        this.withWordTimeOffsets = withWordTimeOffsets;
    }

    static public SpeechToTextOptions buildDefaultOptions() {
        return new SpeechToTextOptions(true, false);
    }

    public boolean isWithPunctuation() {
        return withPunctuation;
    }

    public void setWithPunctuation(boolean withPunctuation) {
        this.withPunctuation = withPunctuation;
    }

    public boolean isWithWordTimeOffsets() {
        return withWordTimeOffsets;
    }

    public void setWithWordTimeOffsets(boolean withWordTimeOffsets) {
        this.withWordTimeOffsets = withWordTimeOffsets;
    }

}
