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
package org.nuxeo.labs;

/**
 * @since 10.2
 *
 */
public interface SpeechToTextResponse {

	/**
	 * A quick way to get the text without other informations.
	 * <br/>
	 * Return the most relevant transcript (§when a Cloud service may return alternatives)
	 * 
	 * @return the text of the transcript
	 */
	String getText();

	/**
	 * 
	 * @return the native object returned by the service provider
	 */
	Object getNativeResponse();

}
