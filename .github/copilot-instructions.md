# GitHub Copilot Instructions

- Before running a Gradle build, 'cd' into project directory
- After making any changes, please check for any warnings or errors introduced by those changes and fix if needed; first check using get_errors tool, once that no longer reports any errors, run full gradle build
- Avoid duplication; if needed, refactor to allow for a more generic solution
- Prefer imports over fully qualified  class names
- Prefer short methods by splitting into multiple methods or utilizing modern Java features, optimize for human readibility
- Prefer short methods; split if necessary
- Utilize Java 17 features like Streams to minimize/optimize code
- Don't add comments that describe changes, like 'New ...', 'Updated ...'. Comments should only be used to explain code if necessary