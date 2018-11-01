# nuxeo-speechtotext

This plug-in uses [Google Cloud Speech-to-Text](https://cloud.google.com/speech-to-text/) to render an audio file to text

**This is Work In Progress**

## Important
1. Please, read the *Support* part, below.2. The plugin is _an example_, showing how to connect to a Google Cloud service not meant to be used with audio files lasting dozens of minutes or more.3. So, there is a limit. And sending an audio file longer than a minute will generate an error, "Sync input too long. For audio longer than 1 min use LongRunningRecognize with a 'uri' parameter."### Warning: Using Google _beta_  version of Speech to Text* In this implementation, the plugin uses Google Speech to Text API _in its BETA VERSION_ (see the pom.xml file for the version).* Google makes it clear that some API may change their billing process, for example, the access to a punctuated text. See the [quota](https://cloud.google.com/speech-to-text/quotas) documentation.## Authentication to Google Cloud ServiceAs of today, only _Service Accounts_  are supported (not API Keys). To set up the credentials, the plugin looks:1. First for a `google.speechtotext.credentials` parameter in `nuxeo.conf`2. If not found there, it checks for an environment variable named `GOOGLE_SPEECHTOTEXT_CREDENTIALS`For unit testing only, if not found in both places above, it looks for a file named `credentials.json`, located in the `resources` folder in the `test` folder. This file is ignored by git, so is not pushed to the repository but **we strongly advise in using an environment variable for unit tests**.## Usage[TBD]Please, read Google's [best practices for Speech to Text API](https://cloud.google.com/speech-to-text/docs/best-practices) (For example, mp3 files are not supported and must be converted, ideally to FLAC)The plugin exposes API allowing to:* Automatically convert to FLAC (the plugin contributes a `commandLine base converter`) before sending the audio file to the service* _Or_ the caller can specify the encoding and rate Hz of the input file (no conversion performed by the plugin)


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