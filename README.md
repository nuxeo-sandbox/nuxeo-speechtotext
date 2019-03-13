# nuxeo-speechtotext

QA status<br/>
[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=Sandbox/sandbox_nuxeo-speechtotext-master)](https://qa.nuxeo.org/jenkins/view/Sandbox/job/Sandbox/job/sandbox_nuxeo-speechtotext-master/)

This plug-in uses [Google Cloud Speech-to-Text](https://cloud.google.com/speech-to-text/) **version v1p1beta1** to render an audio file to text, and it uses it _via_ the REST API (not the JAVA Google Speech-to-Text SDK).



## Important
1. Please, read the *Support* part, below.
2. The plugin is _an example_, showing how to connect to a Google Cloud service not meant to be used with audio files lasting dozens of minutes or more.
3. So, there are known limitations:
  * The call is _synchronous_
  * For audio files of maximum 60 seconds
    * If the file is too big or too long Google returns an error: "Sync input too long. For audio longer than 1 min use LongRunningRecognize with a 'uri' parameter."
  * Please, read Google's [best practices for Speech to Text API](https://cloud.google.com/speech-to-text/docs/best-practices) to check what is supported. For example, mp3 files are not supported and must be converted, ideally to FLAC.

### WARNING: Using Google _beta_  version of Speech to Text
* In this implementation, the plugin uses Google Speech to Text API _in its BETA VERSION_.
* Google makes it clear that some API may change their billing process, for example, the access to a punctuated text. See the [quota](https://cloud.google.com/speech-to-text/quotas) documentation.

## Authentication to Google Cloud Service
As of today, the plug-in only uses an _API KEY_ (not a _Service Accounts_ file). To set up the credentials, the plugin looks:

1. First for a `google.speechtotext.apikey` parameter in `nuxeo.conf`
2. If not found there, it checks for an environment variable named `GOOGLE_SPEECHTOTEXT_APIKEY`

This goes also for unit testing: Set the `GOOGLE_SPEECHTOTEXT_APIKEY` environement variable in your terminal before unit-testing it (either via maven or Eclipse/IntelliJ)

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
* Runs the Speech-to-text and set the result in a Context variable whose name is passed as parameter. This variable is the `SpeechToTextResponse` Java response from the service and its methods can be called:
  * `getText()` returns the first transcript, with or without punctuation (depends on parameters)
  * `getWordTimeOffsets()` returns a JSON array of objects, each object has a "word", a "start" and an "end" fields. "start" and "end" are the number of seconds. (Google can also return nanoseconds, the plugin makes it simpler and returns only seconds)
  * `getNativeResponse()`: Returns the native response encapsulated in a `JSONObject`. In current implementation, the plugin uses only REST to call the service. The result is described in [Google Documentation](https://cloud.google.com/speech-to-text/docs/reference/rest/v1/speech/recognize)
* **Parameters**:
  * `languageCode`(String, **required**): The language code of the audio file (see Google documentation for supported languages))
  * `audioEncoding`(String): The audio encoding as String. See Google documentation for supported encodings.
    * See Google's enumeration. As of 2018-11-03, we have: `"FLAC"`, `"LINEAR16"`, `"MULAW"`, `"AMR"`, `"AMR_WB"`, `"OGG_OPUS"`, and `"SPEEX_WITH_HEADER_BYTE"`.
    * Optional. If the audio file is FLAC or WAW, the parameter is not required
  * `sampleRateHertz`(integer): The rate of the audio file. _Optional_. If the audio file is FLAC or WAW, the parameter is not required.
  *  `withPunctuation`: A `boolean`, optional, default value is `true`. If `false`, the text will be returned with no punctuation.
  *  `withWordTimeOffets`: A `boolean`, optional, default value is `false`. If `true`, `getWordTimeOffsets()` will return a JSON array of objects, each object having the word, and the start/end time (in seconds). This array will be available in the `resultVarName` response
  *  `moreOptionsJSONStr` (String, optional). Add more configuration parameters to send to the service. The plug-in does not encapsulate and handle all and every features of the provider. Passing more parameter is a way to get the results the plug-in does not fetch by default. See the provider REST API documentation (for the current version, see above) 
  *  `resultVarName` (String, **required**): The Name of a Context Variable that will contain the `SpeechToTextResponse` object (see above)
 
 Reminder: Before calling this operation, you can, if needed, convert the audio (or video) to `FLAC` using the "audio-to-flac" commandLine converter provided byu the plugin

#### `Convert.SpeechToTextForDocument`

Converts a blob of the input document and save the transcript to a field of the input document. The file will be automatically  converted to FLAC if needed, before being sent to the service.

* **Category**: `Conversion`
* **Input**: A Document
* **Output**: The modified Document
* Extracts the blob stored in the `blobXpath` parameter (default "file:content"), transcripts it using the required `languageCode` parameter, and stores the transcription in the `transcriptXpath` field of the Document. _Always use punctuation_. See below for more details on parameters.
* **Parameters**:
  * `languageCode`(String, **required**): The language code of the audio file (see Google documentation for supported languages))
  *  `blobXpath`: Source blob to convert
  *  `transcriptXpath`:  Destination `String` field to store the result of the transcript
  *  `withPunctuation`: A `boolean`, optional, default value is `true`. If `false`, the text will be returned with no punctuation.
  *  `withWordTimeOffets`: A `boolean`, optional, default value is `false`. If `true`, `getWordTimeOffsets()` will return a JSON array of objects, each object having the word, and the start/end time (in seconds). This array will be available in the `resultVarName` response
  *  `moreOptionsJSONStr` (String, optional). Add more configuration parameters to send to the service. The plug-in does not encapsulate and handle all and every features of the provider. Passing more parameter is a way to get the results the plug-in does not fetch by default. See the provider REST API documentation (for the current version, see above) 
  *  `saveDocument` (optional). A `boolean`. If `true`, Document is saved (default is `false`).
  *  `resultVarName` (optional): The name of a Context Variable that will contain the `SpeechToTextResponse` object (see above)

## Requirements

Building requires the following software:

* git
* maven

Running the plugin requires Google Cloud API Key to access their Cloud Services.


## Build

    git clone https://github.com/nuxeo-sandbox/nuxeo-speechtotext.git
    cd nuxeo-speechtotext.git
    
    mvn clean install

Note: See _Authentication to Google Cloud Service_. If no Google API Key is provided, the unit tests calling the service are ignored.

## Support

**These features are not part of the Nuxeo Production platform, they are not supportes**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


# Licensing

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)


# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.

More information is available at [www.nuxeo.com](http://www.nuxeo.com).  
