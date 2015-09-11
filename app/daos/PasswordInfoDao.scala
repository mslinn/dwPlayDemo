package daos

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import models.User, User._

class PasswordInfoDao extends DelegableAuthInfoDAO[PasswordInfo] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  def find(loginInfo:LoginInfo):Future[Option[PasswordInfo]] = for {
    user <- users.find(Json.obj(
      "profiles.loginInfo" -> loginInfo
    )).one[User]
  } yield user.flatMap(_.profiles.find(_.loginInfo == loginInfo)).flatMap(_.passwordInfo)

  def add(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo] = 
    users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo
    ), Json.obj(
      "$set" -> Json.obj("profiles.$.passwordInfo" -> authInfo)
    )).map(_ => authInfo)

  def update(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo] = 
    add(loginInfo, authInfo)

  def save(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo] = 
    add(loginInfo, authInfo)

  def remove(loginInfo:LoginInfo):Future[Unit] = 
    users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo
    ), Json.obj(
      "$pull" -> Json.obj(
        "profiles" -> Json.obj("loginInfo" -> loginInfo)
      )
    )).map(_ => ())
}
