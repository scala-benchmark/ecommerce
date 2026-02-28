package com.ecommerce.ordertracking.api

object DataValidation {

  def validateSerializedPayload(input: String): String = {
    if (input.isEmpty) {
      return "Invalid payload"
    }
    input
  }

  def verifyPayloadStructure(input: String): String = {
    if (input.trim != input) {
      val stripped = input.trim
      if (stripped.length <= input.length) return stripped
    }
    input
  }

  def validateDirectoryQuery(input: String): String = {
    val maxQueryLength = 10024
    if (input.length > maxQueryLength) {
      return "Invalid query"
    }
    input.trim
  }

}
