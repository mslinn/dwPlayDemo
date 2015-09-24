dwPlayDemo
==========

This is a [Play framework](https://www.playframework.com/) starter application written in Scala. 
It implements user management and authentication with [Silhouette](http://silhouette.mohiva.com/) 
and uses [MongoDB](https://www.mongodb.org/) for user storage. The application provides:

  * Email-based user sign up flow.
  * Email-based password reset flow.
  * Authentication using credentials (email + password).
  * Account linking: link your Twitter profile to your credentials profile.
  * OAuth1 Twitter authentication.
  * Anonymous or authenticated access to home page.
  * Profile information for authenticated users.

Besides Silhouette, the code depends on the following Play-unrelated projects:
  * [webjars-play](https://github.com/webjars/webjars-play), gives Plays access to [webjars](http://www.webjars.org/).
  * [scala-guide](https://github.com/codingwell/scala-guice), Scala extensions for Google Guice.
  * [ficus](https://github.com/ceedubs/ficus), Scala-friendly access to Play configuration.
  * [play-bootstrap3](https://github.com/adrianhurt/play-bootstrap3), form helpers for Play and Boostrap 3.
  * [play-reactivemongo](https://github.com/ReactiveMongo/Play-ReactiveMongo), Play reactive driver for MongoDB.

On the client side, the application uses [Bootstrap 3](http://getbootstrap.com/) and [JQuery](https://jquery.com/).

The code is loosely based on the [play-multidomain-auth](https://github.com/adrianhurt/play-multidomain-auth) and 
[play-silhouette-reactivemongo-seed](https://github.com/ezzahraoui/play-silhouette-reactivemongo-seed) projects, 
with many improvements:

  * User model designed for account linking
  * Account linking
  * DAO unit testing
  * Check for completed registration before authentication
  * CSRF protection
  * Demo for secure Ajax call
  * Setup for deployment to Bluemix or CloudFoundry
  * HTTP disabled in production mode (only HTTPS allowed) 

All the material is distributed under the Apache 2.0 license (see [LICENSE](./LICENSE)).

This application is [available in Bluemix](https://dwplaydemo.mybluemix.net).
