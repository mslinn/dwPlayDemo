package utils

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.i18n.Messages
import play.api.libs.mailer._

import models.Profile

class Mailer @Inject() (configuration:Configuration, mailer:MailerClient) {
  val from = configuration.getString("mail.from").get
  val replyTo = configuration.getString("mail.reply")

  def sendEmailAsync(recipients:String*)(subject:String, bodyHtml:Option[String], bodyText:Option[String]) = {
    Future {
      sendEmail(recipients:_*)(subject, bodyHtml, bodyText)
    } recover {
      case e => play.api.Logger.error("error sending email", e)
    }
  }

  def sendEmail(recipients:String*)(subject:String, bodyHtml:Option[String], bodyText:Option[String]) {
        val email = Email(subject = subject, from = from, to = recipients, bodyHtml = bodyHtml, bodyText = bodyText, replyTo = replyTo)
        mailer.send(email)
        ()
  }

  def welcome(profile:Profile, link:String)(implicit messages:Messages) = {
    sendEmailAsync(profile.email.get)(
      subject = Messages("mail.welcome.subject"), 
      bodyHtml = Some(views.html.mails.welcome(profile.firstName.get, link).toString),
      bodyText = Some(views.html.mails.welcomeText(profile.firstName.get, link).toString)
    )
  }

  def resetPassword(email:String, link:String)(implicit messages:Messages) = {
    sendEmailAsync(email)(
      subject = Messages("mail.reset.subject"), 
      bodyHtml = Some(views.html.mails.resetPassword(email, link).toString),
      bodyText = Some(views.html.mails.resetPasswordText(email, link).toString)
    )
  }
}