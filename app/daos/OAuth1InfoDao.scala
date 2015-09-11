package daos

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import models.User, User._

class OAuth1InfoDao extends DelegableAuthInfoDAO[OAuth1Info] {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  def find(loginInfo:LoginInfo):Future[Option[OAuth1Info]] = for {
    user <- users.find(Json.obj(
      "profiles.loginInfo" -> loginInfo
    )).one[User]
  } yield user.flatMap(_.profiles.find(_.loginInfo == loginInfo)).flatMap(_.oauth1Info)

  def add(loginInfo:LoginInfo, authInfo:OAuth1Info):Future[OAuth1Info] = 
    users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo
    ), Json.obj(
      "$set" -> Json.obj("profiles.$.oauth1Info" -> authInfo)
    )).map(_ => authInfo)

  def update(loginInfo:LoginInfo, authInfo:OAuth1Info):Future[OAuth1Info] = 
    add(loginInfo, authInfo)

  def save(loginInfo:LoginInfo, authInfo:OAuth1Info):Future[OAuth1Info] = 
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
