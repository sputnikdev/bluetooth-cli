# bluetooth-cli

A test application for the Bluetooth Manager. It is not by any means production-ready application. 
The main purpose of this application is to provide easy-to-use testing platform for the Bluetooth Manager.

---
## Contribution

You are welcome to contribute to the project, the project environment is designed to make it easy by using:
* Travis CI to release artifacts directly to the Maven Central repository.
* Code style rules to support clarity and supportability. The results can be seen in the Codacy. 
* Code coverage reports in the Coveralls to maintain sustainability. 100% of code coverage with unittests is the target.

The build process is streamlined by using standard maven tools. 

To build the project you will need to install the TinyB library into your maven repository. Run this in the root of the project:
```sh
sh .travis/install-dependencies.sh
```

Then build the project with maven:
```bash
mvn clean install
```

To cut a new release and upload it to the Maven Central Repository:
```bash
mvn release:prepare -B
mvn release:perform
```
Travis CI process will take care of everything, you will find a new artifact in the Maven Central repository when the release process finishes successfully.