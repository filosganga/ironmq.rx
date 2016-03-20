package com.github.filosganga.ironmqrx

import io.circe.{Json, Encoder, Decoder}
import io.circe.syntax._

trait Codec {

  implicit val messageIdEncoder: Encoder[Message.Id] = Encoder.instance { id =>
    Json.string(id.value)
  }

  implicit val messageIdDecoder: Decoder[Message.Id] = Decoder.instance { cursor =>
    cursor.as[String].map(Message.Id.apply)
  }

  implicit val reservationIdEncoder: Encoder[Reservation.Id] = Encoder.instance { id =>
    Json.string(id.value)
  }

  implicit val reservationIdDecoder: Decoder[Reservation.Id] = Decoder.instance { cursor =>
    cursor.as[String].map(Reservation.Id.apply)
  }

  implicit val messageIdsDecoder: Decoder[Message.Ids] = Decoder.instance { cursor =>
    cursor.downField("ids").as[List[Message.Id]].map(Message.Ids.apply)
  }

  implicit val messageDecoder: Decoder[Message] = Decoder.instance { cursor =>
    for {
      id <- cursor.downField("id").as[Message.Id]
      body <- cursor.downField("body").as[String]
      noOfReservations <- cursor.downField("reserved_count").as[Int]
    } yield Message(id, body, noOfReservations)
  }

  implicit val reservedMessageDecoder: Decoder[ReservedMessage] = Decoder.instance { cursor =>
    for {
      message <- cursor.as[Message]
      reservationId <- cursor.downField("reservation_id").as[Reservation.Id]
    } yield ReservedMessage(reservationId, message)
  }

  implicit val reservationEncoder: Encoder[Reservation] = Encoder.instance { r =>
    Json.obj("id"->r.messageId.asJson, "reservation_id"->r.reservationId.asJson)
  }
}

object Codec extends Codec
