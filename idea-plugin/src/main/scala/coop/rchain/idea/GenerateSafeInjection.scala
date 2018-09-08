package coop.rchain.idea

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{
  ScClass,
  ScObject,
  ScTypeDefinition
}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector
import org.jetbrains.plugins.scala.lang.psi.types._

class GenerateSafeInjection extends SyntheticMembersInjector {
  private val logger = Logger.getInstance(classOf[GenerateSafeInjection])

  private def convertToSafeTpe(tpe: ScType): Option[String] =
    tpe.extractClass.map(_.getName).map(name => tpe.canonicalText + "." + name + "Safe")

  override def injectInners(source: ScTypeDefinition): Seq[String] =
    source match {
      case obj: ScObject =>
        ScalaPsiUtil.getCompanionModule(obj) match {
          case Some(clazz: ScClass)
              if clazz.findAnnotationNoAliases("coop.rchain.models.macros.GenerateSafe") != null =>
            val safeClassName = clazz.name + "Safe"
            val safeParamsAndMethods = clazz.parameters.map { parameter =>
              parameter.`type`() match {
                case Right(ScParameterizedType(des, Seq(tpe)))
                    if des.canonicalText == "_root_.scala.Option" =>
                  convertToSafeTpe(tpe).map { safeTpe =>
                    (
                      parameter.name + ": " + safeTpe,
                      s"def with${parameter.name.capitalize}(v: $safeTpe): $safeClassName = ???"
                    )
                  }
                case Right(tpe) =>
                  Some(
                    (
                      parameter.name + ": " + tpe.canonicalText,
                      s"def with${parameter.name.capitalize}(v: ${tpe.canonicalText}): $safeClassName = ???"
                    )
                  )
                case Left(_) =>
                  None
              }
            }
            if (safeParamsAndMethods.forall(_.isDefined)) {
              val (safeParams, withMethods) = safeParamsAndMethods.map(_.get).unzip
              val generatedParams           = safeParams :+ s"underlying: ${clazz.name}"
              logger.info(s"Class ${clazz.name} has safe params: ${generatedParams.mkString(", ")}")
              Seq(
                s"""final case class $safeClassName (${generatedParams.mkString(", ")}) {
                   |  def toByteString: com.google.protobuf.ByteString = ???
                   |  def toByteArray: Array[Byte] = ???
                   |  ${withMethods.mkString("\n")}
                   |}
                 """.stripMargin,
                s"""object $safeClassName {
                   |  def create(underlying: ${clazz.name}): Option[$safeClassName] = ???
                   |}
                 """.stripMargin
              )
            } else {
              logger.warn(s"Couldn't derive some of parameters types for ${clazz.name}")
              Seq.empty
            }
          case _ => Seq.empty
        }
      case _ => Seq.empty
    }
}
