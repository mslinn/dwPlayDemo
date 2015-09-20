package daos

import java.util.UUID

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

import models.UserToken

trait UserTokenDao {
  def find(id:UUID):Future[Option[UserToken]]
  def save(token:UserToken):Future[UserToken]
  def remove(id:UUID):Future[Unit]
}

class MongoUserTokenDao extends UserTokenDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val tokens = reactiveMongoApi.db.collection[JSONCollection]("tokens")

  def find(id:UUID):Future[Option[UserToken]] = 
      tokens.find(Json.obj("id" -> id)).one[UserToken]

  def save(token:UserToken):Future[UserToken] = for {
    _ <- tokens.insert(token)
  } yield token
       
  def remove(id:UUID):Future[Unit] = for {
    _ <- tokens.remove(Json.obj("id" -> id))
  } yield ()
}
