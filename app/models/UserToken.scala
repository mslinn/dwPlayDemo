package models

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json.Json

case class UserToken(id:UUID, userId:UUID, email:String, expirationTime:DateTime, isSignUp:Boolean) {
  def isExpired = expirationTime.isBeforeNow
}

object UserToken {
  implicit val toJson = Json.format[UserToken]
  
  def create(userId:UUID, email:String, isSignUp:Boolean) = 
    UserToken(UUID.randomUUID(), userId, email, new DateTime().plusHours(12), isSignUp)
}
