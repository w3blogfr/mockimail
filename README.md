mockimail
=========

Catch all your mail during  development

Installation
==================

Clone directory



Jetty Embedded (Master)
==================

I made a new version to remove Spring. I prefer use Jetty Embedded to delivery 3 static pages and 1 dynamic request.
Project is less fat and could be probably deployed quickly on the cloud.

	mvn exec:java


Old Webapps (Spring Branch)
==================

	mvn jetty:run

By default, smtp port is 5000

Application is here : http://localhost:8080/
