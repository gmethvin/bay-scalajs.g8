package bay.driver

import cats.syntax.either._
import com.github.tminglei.slickpg._
import slick.jdbc.{JdbcType, PositionedResult}

import scala.reflect.classTag

trait NewPgCirceJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: ExPostgresDriver =>
  import driver.api._
  import io.circe._
  import io.circe.parser._
  import io.circe.syntax._

  ///---
  def pgjson: String
  ///---

  trait CirceCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("json", classTag[Json])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("jsonb", classTag[Json])
    }
  }

  trait JsonImplicits extends CirceImplicits

  trait CirceImplicits extends CirceCodeGenSupport {
    implicit val circeJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        pgjson,
        (v) => parse(v).getOrElse(Json.Null),
        (v) => v.asJson.spaces2,
        hasLiteralForm = false
      )

    implicit def circeJsonColumnExtensionMethods(c: Rep[Json]): JsonColumnExtensionMethods[Json, Json] = {
      new JsonColumnExtensionMethods[Json, Json](c)
    }

    implicit def circeJsonOptionColumnExtensionMethods(c: Rep[Option[Json]]): JsonColumnExtensionMethods[Json, Option[Json]] = {
      new JsonColumnExtensionMethods[Json, Option[Json]](c)
    }
  }

  trait CirceJsonPlainImplicits extends CirceCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionResult(r: PositionedResult) {
      def nextJson(): Json               = nextJsonOption().getOrElse(Json.Null)
      def nextJsonOption(): Option[Json] = r.nextStringOption().map(parse(_).getOrElse(Json.Null))
    }

    implicit val getJson       = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson       = mkSetParameter[Json](pgjson, _.asJson.spaces2)
    implicit val setJsonOption = mkOptionSetParameter[Json](pgjson, _.asJson.spaces2)
  }
}
