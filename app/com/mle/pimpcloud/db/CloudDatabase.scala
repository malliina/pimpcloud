package com.mle.pimpcloud.db

import java.sql.SQLException
import java.util.UUID

import com.mle.db.DatabaseLike
import com.mle.pimpcloud.CloudIdentityStore
import com.mle.pimpcloud.db.CloudDatabase.AlreadyExists
import com.mle.util.Log
import org.h2.jdbcx.JdbcConnectionPool

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class CloudDatabase(name: String) extends DatabaseLike with CloudIdentityStore with Log {
  val ids = TableQuery[CloudIDs]

  val pool = JdbcConnectionPool.create(s"jdbc:h2:~/.pimpcloud/$name;DB_CLOSE_DELAY=-1", "", "")
  override val database = Database.forDataSource(pool)
  override val tableQueries = Seq(ids)
  init()

  def newID(): String = UUID.randomUUID().toString take 5

  def exists(id: String) = withSession(s => ids.filter(_.id === id).run(s)).nonEmpty

  /**
   * @return a unique ID if successfully generated and saved, [[None]] otherwise
   */
  def generateAndSave(): Either[AlreadyExists, String] = trySave(newID())

  /**
   *
   * @param id
   * @return `id` if successfully added, None if `id` already exists
   */
  def trySave(id: String): Either[AlreadyExists, String] =
    try {
      withSession(implicit s => ids += id)
      Right(id)
    } catch {
      case sqle: SQLException if sqle.getMessage contains "primary key violation" =>
        log.warn(s"Unable to save cloud ID: $id because it already exists.")
        Left(AlreadyExists(id))
    }

  def remove(id: String) = withSession(s => ids.filter(_.id === id).delete(s))

  def close(): Unit = pool.dispose()
}

object CloudDatabase {

  trait DataMessage

  case class AlreadyExists(id: String) extends DataMessage

  val default = new CloudDatabase("clouddb")
}
