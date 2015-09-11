package daos

import scala.concurrent.Await

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable.Specification

class OAuth1InfoDaoSpec extends Specification with DaoSpecResources {

  "OAuth1InfoDao" should {
    "Add new OAuth1 information" in withOAuth1InfoDao { oauth1InfoDao =>
      val future = for {
        _ <- oauth1InfoDao.save(oauth1LoginInfo, oauth1Info)
        maybeOauth1Info <- oauth1InfoDao.find(oauth1LoginInfo)
      } yield maybeOauth1Info.map(_ == oauth1Info)
      Await.result(future, timeout) must beSome(true)
    }

    "Remove existing OAuth1 information" in withOAuth1InfoDao { oauth1InfoDao =>
      val future = for {
        _ <- oauth1InfoDao.save(oauth1LoginInfo, oauth1Info)
        _ <- oauth1InfoDao.remove(oauth1LoginInfo)
        maybeOauth1Info <- oauth1InfoDao.find(oauth1LoginInfo)
      } yield maybeOauth1Info
      Await.result(future, timeout) must beNone       
    }
  }
}
