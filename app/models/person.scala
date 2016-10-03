package models

import org.joda.time.{DateTime, Years}
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

import scala.collection.mutable

sealed trait Sex
case object male extends Sex
case object female extends Sex

sealed trait AddressType
case object personal extends AddressType
case object professional extends AddressType


case class Person(name: String, lastName: String, birthDate: DateTime, sex: Sex, addresses: Map[AddressType, Address] = Map.empty) {
  
  def age: Int = Years.yearsBetween(birthDate, DateTime.now(birthDate.getZone)).getYears

  def professionalAddress: Option[Address] = addresses get professional

  def personalAddress: Option[Address]  = addresses get personal

}

object Person {

  implicit val addressFormat = Address.addressFormat
  implicit val addressTypeFormat = AddressType.AddressTypeFormat

  implicit object addressesFormat extends Format[Map[AddressType, Address]] {

    import play.api.libs.functional.syntax._ // Combinator syntax

    override def reads(json: JsValue): JsResult[Map[AddressType, Address]] = json.validate[Map[AddressType, Address]](
      (
        (JsPath \ personal.toString).readNullable[Address] and
        (JsPath \ professional.toString).readNullable[Address]
      )(
        (personalAddress, professionalAddress) => {
          val addresses: mutable.Map[AddressType, Address] = mutable.Map.empty
          if (personalAddress.isDefined) addresses += (personal -> personalAddress.get)
          if (professionalAddress.isDefined) addresses += (professional -> professionalAddress.get)
          addresses.toMap
        }
      )
    )

    override def writes(o: Map[AddressType, Address]): JsValue = Json.obj(
      o.map {
        case (addressType, address) =>
          val ret: (String, JsValueWrapper) = addressTypeFormat.writes(addressType).as[String] -> addressFormat.writes(address)
          ret
      }.toSeq:_*
    )

  }

  implicit val sexFormat = Sex.SexFormat

  implicit val personFormat = Json.format[Person]

}

object AddressType {

  object AddressTypeFormat extends Format[AddressType] {

    override def reads(json: JsValue): JsResult[AddressType] = json match {
      case JsString("personal")      => JsSuccess(personal)
      case JsString("professional")  => JsSuccess(professional)
      case _                         => JsError("could not parse: " + json)
    }

    override def writes(o: AddressType): JsValue = JsString(o.toString)

  }

}

object Sex {

  object SexFormat extends Format[Sex] {

    override def reads(json: JsValue): JsResult[Sex] = json match {
      case JsString("male")    => JsSuccess(male)
      case JsString("female")  => JsSuccess(female)
      case _                   => JsError("could not parse: " + json)
    }

    override def writes(o: Sex): JsValue = JsString(o.toString)

  }

}