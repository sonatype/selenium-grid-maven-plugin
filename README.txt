All examples assume you are testing from this project directory with a 1.2-SNAPSHOT version.

Selenium 1x:
Start a blocking hub and remote control servers:
mvn org.sonatype.maven.plugin:selenium-grid-maven-plugin:1.2-SNAPSHOT:start-grid -Dselenium-grid.block=true -Dselenium-grid.silent=false -Dselenium.environment=*firefox

After it starts, make note of the selenium-ports that are used. Look for line like
[INFO] selenium-grid-maven-plugin: Storing ports as selenium-ports=63447,63454,63472

Test the hub or remotes by using one of the ports output from above command (example 63454) in a nother terminal:
mvn org.sonatype.maven.plugin:selenium-grid-maven-plugin:1.2-SNAPSHOT:test-grid -Dselenium-grid:silent=false -Dselenium-ports=63454 -Dselenium.environment=*firefox

Webdriver/Selenium:
Use the same commands above, except append the following to the mvn comand line:
-Dwebdriver=true

