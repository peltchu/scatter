/*
 * Copyright 2012 @peltchu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.scatter


import cc.spray.json._
import cc.spray.typeconversion.SprayJsonSupport
import cc.spray.typeconversion._
import cc.spray.typeconversion.DefaultUnmarshallers._
import cc.spray.json.DefaultJsonProtocol._
import util.matching.Regex
import cc.spray.http._

trait Validations {

	/**
	 * Validates that response has exactly the given json
	 * @param data the data that response json is matched against
	 * @tparam A the type which response json is attempted to be unmarshalled to
	 */
	case class hasExactJson[A: RootJsonReader](data: A) extends ContentValidation {
		def validateContent(content: HttpContent) = {
			import cc.spray.typeconversion.SprayJsonSupport._
			sprayJsonUnmarshaller[A].apply(Some(content)) match {
				case Left(e) =>
					throw new ValidationException("hasExactJson: Deserialization error (" + e.toString + ")")
				case Right(res) => ValidationResult(res == data, Pair("hasJson", "[" + res.getClass.getName + "]"))
			}
		}
	}

	/**
	 * Validates that response json fulfills the given predicate
	 *
	 * Example:
	 * {{{
	 * hasJson[Person] (p => p.name == "John Doe")
	 * }}}
	 *
	 * @param f the predicate
	 * @tparam A the type which response content is attempted to be unmarshalled to
	 */
	case class hasJson[A: RootJsonReader](f: A => Boolean) extends ContentValidation {
		def validateContent(content: HttpContent) = {
			import cc.spray.typeconversion.SprayJsonSupport._
			sprayJsonUnmarshaller[A].apply(Some(content)) match {
				case Left(e) =>
					throw new ValidationException("hasJson: Deserialization error (" + e.toString + ")")
				case Right(res) => ValidationResult(f(res), Pair("hasJson", "[" + res.getClass.getName + "]"))
			}
		}
	}

	/**
	 * Validates that response has the given status
	 *
	 * See also the companion object for alternative methods of creation
	 * @param status HTTP status code
	 */
	case class hasStatus(status: Int) extends ResponseValidation {
		def validate(response: HttpResponse) =
			ValidationResult(response.status.value == status,
				Pair("hasStatus", status.toString))
	}

	/**
	 * Provides alternative methods for creating hasStatus
	 */
	object hasStatus {
		/**
		 * Validates that response has the given status
		 * @param code HTTP status code
		 * @return
		 */
		def apply(code: StatusCode) = {
			new hasStatus(code.value)
		}
	}


	/**
	 * Helper class for supporting multiple kinds of header validations
	 * @param name header name
	 * @param vd header value validator function
	 */
	protected[scatter] case class hasHeaderValidation(name: String, vd: (String => Boolean)) extends ResponseValidation {
		def validate(response: HttpResponse) = {
			response.headers.find(_.name.toLowerCase == name.toLowerCase) match {
				case Some(HttpHeader(n, v)) => ValidationResult(vd(v), Pair("hasHeader", name + " : " + v))
				case None => ValidationResult(false, Pair("hasHeader", name))
			}
		}
	}

	/**
	 * Validates that response has the given header
	 */
	object hasHeader {

		/**
		 * Validates that the given header and header value exists
		 * @param name header name
		 * @param value header value
		 * @return
		 */
		def apply(name: String, value: String) = {
			new hasHeaderValidation(name, (_ == value))
		}

		/**
		 * Validates that the given header exists and that its value matches the regex
		 * @param name header name
		 * @param value header value regex matcher
		 * @return
		 */
		def apply(name: String, value: Regex) = {
			new hasHeaderValidation(name, (value.findFirstIn(_).nonEmpty))
		}

		/**
		 * Validates that the given header exists
		 * @param header the header
		 * @return
		 */
		def apply(header: HttpHeader) = {
			new hasHeaderValidation(header.name, (_ == header.value))
		}
	}


	/**
	 * Validates that response has the given charset
	 * @param charset the charset as string
	 *
	 * See also the companion object for alternative methods of creation
	 */
	case class hasCharset(charset: String) extends ContentValidation {
		def validateContent(content: HttpContent) = {
			content.contentType.charset match {
				case Some(cs) =>
					val names = cs.value :: cs.aliases.toList
					names.find(_.toLowerCase == charset.toLowerCase) match {
						case Some(x) => ValidationResult(true, Pair("hasCharset", charset))
						case None => ValidationResult(false, Pair("hasCharset", charset))
					}
				case None =>
					// NOTE: according to http spec ISO-8859-1 should be considered as default
					// if no charset is specified. Thus, we could accept empty charset definition
					// if the given validation charset was ISO-8859-1. However, as the hasCharset
					// implies explicitly validating for given charset, we'll consider undefined
					// charset as an error situation here.
					throw new ValidationException("hasCharset: Cannot validate charset: " +
						"Response has no charset defined")
			}
		}
	}

	/**
	 * Provides alternative methods for creating hasCharset
	 */
	object hasCharset {
		/**
		 * Validates that response has the given charset
		 * @param charset the charset
		 * @return
		 */
		def apply(charset: HttpCharset) = {
			new hasCharset(charset.value)
		}
	}


	/**
	 * Validates that response has the given media type
	 *
	 * See also the companion object for alternative methods of creation
	 * @param mtype the media type
	 */
	case class hasMediaType(mtype: String) extends ContentValidation {
		def validateContent(content: HttpContent) =
			content.contentType.mediaType.value match {
				case `mtype` => ValidationResult(true, Pair("hasMediaType", mtype))
				case x => ValidationResult(false, Pair("hasMediaType", mtype))
			}
	}

	/**
	 * Provides alternative methods for creating hasMediaType
	 */
	object hasMediaType {
		/**
		 * Validates that response has the given media type
		 * @param mtype the media type
		 * @return
		 */
		def apply(mtype: MediaType) = {
			new hasMediaType(mtype.value)
		}
	}

	/**
	 * Validates that response has no content
	 */
	case object hasNoContent extends ResponseValidation {
		def validate(response: HttpResponse) =
			ValidationResult(response.content.isEmpty, Pair("hasNoContent", ""))
	}

	/**
	 * Helper class for supporting multiple kinds of content text validations
	 * @param vd the validator function
	 */
	private[scatter] case class hasTextValidation(vd: (String => Boolean)) extends ContentValidation {
		def validateContent(content: HttpContent) = {
			import java.nio.charset.Charset
			import cc.spray.http.HttpCharsets.`ISO-8859-1`
			val c = new String(
				content.buffer,
				Charset.forName(content.contentType.charset.getOrElse(`ISO-8859-1`).value)
			)
			ValidationResult(vd(c), Pair("hasText", "[String]"))
		}
	}

	/**
	 * Validates that response content, when unmarshalled as string, has the given text
	 */
	object hasText {
		/**
		 * Validates that content is exactly the given string
		 * @param text content as string
		 * @return
		 */
		def apply(text: String) = {
			new hasTextValidation((_ == text))
		}

		/**
		 * Validates that content matches the given regex
		 * @param rx the regex
		 * @return
		 */
		def apply(rx: Regex) = {
			new hasTextValidation((rx.findFirstIn(_).nonEmpty))
		}

	}

	/**
	 * Negates the given validation
	 *
	 * Example:
	 * {{{
	 *  // anything except HTTP status 200
	 *  _not(hasStatus(200))
	 * }}}
	 *
	 * @param validation the validation to negate
	 */
	case class _not(validation: ResponseValidation) extends ResponseValidation {
		def validate(response: HttpResponse) = {
			val r = validation.validate(response)
			ValidationResult(!r.success, Pair("not", r.describe))
		}
	}

	/**
	 * Validates that response matches exactly ONE of the given validations
	 *
	 * Example:
	 * {{{
	 * oneOf(hasStatus(200), hasStatus(201), hasStatus(403))
	 * }}}
	 *
	 * @param v1 the first validation
	 * @param v2 the second validation
	 * @param rest rest of the validations (any number of them)
	 */
	case class oneOf(v1: ResponseValidation, v2: ResponseValidation, rest: ResponseValidation*) extends ResponseValidation {
		def validate(response: HttpResponse) = {
			val validations = v1 :: v2 :: rest.toList
			val vres = validations.foldLeft(List[ValidationResult]())((a, b) => b.validate(response) :: a)
			// only ONE of the validations is allowed to be true
			val success = vres.foldLeft(false)((acc, v) =>
				if (!acc)
					v.success
				else
					!(acc && v.success)
			)

			val values = vres.map(_.describe).mkString(", ")
			ValidationResult(success, Pair("oneOf", "(" + values + ")"))
		}
	}

	/**
	 * Helper class for content validations
	 */
	private[scatter] trait ContentValidation extends ResponseValidation {
		def validate(response: HttpResponse): ValidationResult = {
			response.content match {
				case None => throw new ValidationException("Cannot validate content: Response has no content")
				case Some(con) => validateContent(con)
			}
		}

		def validateContent(content: HttpContent): ValidationResult
	}

}
