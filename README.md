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

This project is inspired by the [play-multidomain-auth](https://github.com/adrianhurt/play-multidomain-auth) and 
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

This application is [available in Bluemix](https://dwplaydemo.mybluemix.net).
