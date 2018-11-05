# nuxeo-speechtotext

QA status<br/>
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-speechtotext-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-speechtotext-master/)

This plug-in uses [Google Cloud Speech-to-Text](https://cloud.google.com/speech-to-text/) to render an audio file to text


## Important
1. Please, read the *Support* part, below.
2. The plugin is _an example_, showing how to connect to a Google Cloud service not meant to be used with audio files lasting dozens of minutes or more.
3. So, there are known limitations:
  * The call is _synchronous_
  * For audio files of maximum 60 seconds
    * If the file is too big or too long Google returns an error: "Sync input too long. For audio longer than 1 min use LongRunningRecognize with a 'uri' parameter."
  * Please, read Google's [best practices for Speech to Text API](https://cloud.google.com/speech-to-text/docs/best-practices) to check what is supported. For example, mp3 files are not supported and must be converted, ideally to FLAC.

### WARNING #1: Using Google _beta_  version of Speech to Text
* In this implementation, the plugin uses Google Speech to Text API _in its BETA VERSION_ (see the pom.xml file for the exact version number).
* Google makes it clear that some API may change their billing process, for example, the access to a punctuated text. See the [quota](https://cloud.google.com/speech-to-text/quotas) documentation.

<hr>
## WARNING #2: Goggle Guava version and Nuxeo 10.2
The Google Java API used by the plugin requires `guava` (Google set of Java utilities) version 21.0 minimum. Nuxeo 10.2 deploys version 18.0: The marketplace package forces the installation of `guava-21.0.jar` (in /{nuxeo-home}/nxserver/lib/) and it _replaces_ the default one (`guava-18.0.jar`). So far, in our testing (not only unit testing, but also testing on a deployed Nuxeo server, etc.) we have not seen any issue, but by essence, this kind of testing is limited.

So, if something goes wrong, check `server.log` to see if it is cause by Google Guava.

**THIS VERSION OF THE PLUGIN IS FOR NUXEO 10.2 ONLY**. Create a branch and test/adapt if you are using another version.

Notice that the problem is solved in the coming 10.3 (~December 2018), which uses version 26.0. So **when 10.3 is released, the package must also be updated** to change the guava version in the pom (and it it is named `guava-{version}-jre.jar` and not `guava-{version}.jar`)

In case of problem with 10.2, it is likely that the fix will be to rewrote the plugin and use only the REST API. WHich probably is not worth it: 10.3 is scheduled very soon (in a month) at the time this README is written.
<hr>

## Authentication to Google Cloud Service
As of today, only _Service Accounts_  are supported (not API Keys) by the Google Java SDK. To set up the credentials, the plugin looks:

1. First for a `google.speechtotext.credentials` parameter in `nuxeo.conf`
2. If not found there, it checks for an environment variable named `GOOGLE_SPEECHTOTEXT_CREDENTIALS`

For unit testing only, if not found in both places above, it looks for a file named `credentials.json`, located in the `resources` folder in the `test` folder. This file is ignored by git, so is not pushed to the repository but **we strongly advise in using an environment variable for unit tests**.

## Usage

Please, read Google's [best practices for Speech to Text API](https://cloud.google.com/speech-to-text/docs/best-practices) (For example, mp3 files are not supported and must be converted, ideally to FLAC)

The plugin exposes API allowing to:

* Automatically convert to FLAC (the plugin contributes a `commandLine base converter`) before sending the audio file to the service
* _Or_ the caller can specify the encoding and rate Hz of the input file (no conversion performed by the plugin)

The plugin does not automatically convert `Audio` (or `Video`) files to text. You will add listeners, buttons... that will call one of the following operations:

#### `Convert.SpeechToTextForBlob`
* **Category**: `Conversion`
* **Input**: A single `Blob`
* **Output** the same `Blob`, unchanged
* Runs the Speech-to_text and set the result in a Context variable whose name is passed as parameter. This variable is the `SpeechToTextResponse` Java response from the service and its methods can be called:
  * `getText()` returns the first transcript, with or without punctuation (depends on parameters)
  * `getWordTimeOffsets()` returns a JSON array of objects, each ojcte has a "word", a "start" and an "end" fields. "start" and "end" are the number of seconds. (Google can also return nanoseconds, the plugin makes it simpler and returns only seconds)
  * `getNativeResponse()`: Returns the native, Google `RecognizeResponse` (see this Object in Google documentation if you need to get more information)
* **Parameters**:
  * `languageCode`(String, **required**): The language code of the audio file (see Google documentation for supported languages))
  * `audioEncoding`(String): The audio encoding as String. See Google documentation for supported encodings.
    * See Google's `RecognitionConfig.AudioEncoding` enumeration. As of 2018-11-03, we have: "FLAC", "LINEAR16", "MULAW", "AMR", "AMR_WB", "OGG_OPUS", and "SPEEX_WITH_HEADER_BYTE".
    * Optional. If the audio file is FLAC or WAW, the parameter is not required
  * `sampleRateHertz`(integer): The rate of the audio file. _Optional_. If the audio file is FLAC or WAW, the parameter is not required.
  *  `withPunctuation`: A `boolean`. If `false`, the text will be returned with no punctuation.
  *  `withWordTimeOffets`: A `boolean`. If `true`, `getWordTimeOffsets()` will return a JSON array of objects, each object having the word, and the start/end time (in seconds).
  *  `resultVarName` (String, **required**): The Name of a Context Variable that will contain the `SpeechToTextResponse` object (see above)
 
 Reminder: Before calling this operation, you can, if needed, convert the audio (or video) to `FLAC` using the "audio-to-flac" commandLine converter provided byu the plugin

#### `Convert.SpeechToTextForDocument`

Converts a blob of the input document and save the transcript to a field of the input document. The file will be automatically  converted to FLAC if needed, before being sent to the service.

* **Category**: `Conversion`
* **Input$**: A Document
* **Output**: The modified Document
* Extracts the blob stored in the `blobXpath` parameter (default "file:content"), transcripts it using the required `languageCode` parameter, and stores the transcription in the `transcriptXpath` field of the Document. _Always use punctuation_. See below for more details on parameters.
* **Parameters**:
  * `languageCode`(String, **required**): The language code of the audio file (see Google documentation for supported languages))
  *  `blobXpath`: Source blob to convert
  *  `transcriptXpath`:  Destination `String` field to store the result of the transcript
  *  `saveDocument` (optional). A `boolean. If `true`, Documen is saved (default is `false`).
  *  `resultVarName (optional): The name of a Context Variable that will contain the `SpeechToTextResponse` object (see above)

## Requirements

Building requires the following software:

* git
* maven

Running the plugin requires Google Cloud credentials to access their Cloud Services.


## Build

    git clone https://github.com/nuxeo-speechtotext.git
    cd nuxeo-speechtotext.git
    
    mvn clean install

## Support

**These features are not part of the Nuxeo Production platform, they are not supportes**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


# Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).  
