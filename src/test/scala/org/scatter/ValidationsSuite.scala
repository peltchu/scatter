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

import org.scalatest.FunSuite
import cc.spray.http.HttpMethods._
import cc.spray.http.HttpHeaders._
import cc.spray.http.CacheDirectives._
import cc.spray.http.HttpCharsets._
import cc.spray.http.MediaTypes._
import org.scalatest.matchers.MustMatchers
import cc.spray.http._

import cc.spray.typeconversion._
import cc.spray.typeconversion.SprayJsonSupport._
import cc.spray.json._
import cc.spray.json.DefaultJsonProtocol._
import cc.spray.http.StatusCodes._

class ValidationsSuite extends FunSuite with MustMatchers with Validations {

	val testResp = HttpResponse(
		200,
		List(HttpHeader("Pragma", "no-cache"), `Cache-Control`(`no-store`)),
		HttpContent(ContentType(`text/plain`, `UTF-8`), "foobar")
	)


	val jsonResp = HttpResponse(
		200,
		List("foo", "bar").toHttpContent
	)

	test("hasStatus") {
		hasStatus(200).validate(testResp).success must be (true)
		hasStatus(OK).validate(testResp).success must be (true)
		hasStatus(400).validate(testResp).success must be (false)
	}

	test("hasHeader") {
		hasHeader("Pragma", "no-cache").validate(testResp).success must be (true)
		hasHeader("pragma", "no-cache").validate(testResp).success must be (true)
		hasHeader("Pragma", "no-cache|public".r).validate(testResp).success must be (true)
		hasHeader(`Cache-Control`(`no-store`)).validate(testResp).success must be (true)
		hasHeader("Pragma", "public").validate(testResp).success must be (false)
		hasHeader("Foo", "bar").validate(testResp).success must be (false)
		hasHeader("Pragma", "no-cache").validate(HttpResponse(200)).success must be (false)
	}

	test("hasCharset") {
		hasCharset("UTF-8").validate(testResp).success must be (true)
		hasCharset("utf-8").validate(testResp).success must be (true)
		hasCharset(`UTF-8`).validate(testResp).success must be (true)
		hasCharset("ISO-8859-1").validate(testResp).success must be (false)
	}

	test("hasCharset (no charset defined)") {
		intercept[ValidationException] {
			hasCharset("UTF-8").validate(HttpResponse(200))
		}
	}

	test("hasMediaType") {
		hasMediaType("text/plain").validate(testResp).success must be (true)
		hasMediaType(`text/plain`).validate(testResp).success must be (true)
		hasMediaType("application/json").validate(testResp).success must be (false)
	}

	test("hasNoContent") {
		hasNoContent.validate(testResp).success must be (false)
		hasNoContent.validate(HttpResponse(200)).success must be (true)
	}

	test("hasText") {
		hasText("foobar").validate(testResp).success must be (true)
		hasText("""^foo.*""".r).validate(testResp).success must be (true)
		hasText("barfoo").validate(testResp).success must be (false)
	}

	test("hasJson") {
		import cc.spray.typeconversion.DefaultUnmarshallers._
		hasJson[List[String]](m =>
			m(0) == "foo").validate(jsonResp).success must be (true)
		hasJson[List[String]](m =>
			m(0) == "bar").validate(jsonResp).success must be (false)
	}
	
	test("hasJson (deserialization error)") {
		intercept[ValidationException] {
			hasJson[Map[String,String]](_.size > 1).validate(jsonResp)
		}
	}

	test("hasExactJson") {
		hasExactJson(List("foo", "bar")).validate(jsonResp).success must be (true)
		hasExactJson(List("foo")).validate(jsonResp).success must be (false)
		hasExactJson(List[String]()).validate(jsonResp).success must be (false)
	}
	
	test("hasExactJson (deserialization error)") {
		intercept[ValidationException] {
			hasExactJson(Map("foo" -> "bar")).validate(jsonResp)
		}
	}

	test("not") {
		_not(hasStatus(200)).validate(testResp).success must be (false)
		_not(hasStatus(400)).validate(testResp).success must be (true)
	}

	test("oneOf") {
		oneOf(hasStatus(200), hasStatus(400)).validate(testResp).success must be (true)
		oneOf(hasStatus(201), hasStatus(400)).validate(testResp).success must be (false)

		oneOf(hasStatus(200), hasStatus(200), hasStatus(400), hasStatus(500)).validate(testResp).success must be (false)
		oneOf(hasStatus(400), hasStatus(500), hasStatus(200), hasStatus(400)).validate(testResp).success must be (true)
		oneOf(hasStatus(400), hasStatus(200), hasStatus(500), hasStatus(200)).validate(testResp).success must be (false)

	}
	
	test("content validation (no content)") {
		object DummyContentValidation extends ContentValidation {
			def validateContent(content: HttpContent) = ValidationResult(false, Pair("foo", "bar"))
		}
		val emptyResponse = HttpResponse(200)

		intercept[ValidationException] {
			DummyContentValidation.validate(emptyResponse)
		}
	}

}
